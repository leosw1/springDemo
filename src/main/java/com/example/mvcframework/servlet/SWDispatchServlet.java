package com.example.mvcframework.servlet;

import com.example.mvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.lang.reflect.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author SW
 * @date create 2019-04-24 12:57
 */
public class SWDispatchServlet extends HttpServlet {

    // 配置信息都存入在properties中
    private Properties p = new Properties();

    private List<String> classNames = new ArrayList<String>();

    private Map<String, Object> ioc = new HashMap<String, Object>();

//    private Map<String, Method> handlerMapping = new HashMap<String, Method>();

    private List<Handler> handlerMapping = new ArrayList<Handler>();

    /**
     * 初始化阶段调用的方法
     *
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        //1. 加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2. 根据配置文件扫描所有的相关的类
        doScanner(p.getProperty("scanPackage"));

        //3. 初始化所有的相关类的实例，并且将其放入到ioc容器之中，也就是Map
        doInstance();

        //4. 实现自动依赖注入
        doAutowrited();

        //5. 初始化handlerMapping
        initHandlerMapping();


        System.out.println("--------- SW MVC INIT-------------");
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(SWController.class)) {
                return;
            }
            String baseUrl = "";
            if (clazz.isAnnotationPresent(SWRequestMapping.class)) {
                SWRequestMapping reqMapping = (SWRequestMapping) clazz.getAnnotation(SWRequestMapping.class);
                baseUrl = reqMapping.value();
            }

//            Method[] methods = clazz.getMethods();
//            for (Method method : methods) {
//                if (!method.isAnnotationPresent(SWRequestMapping.class)) {
//                    continue;
//                }
//                SWRequestMapping reqMapping = method.getAnnotation(SWRequestMapping.class);
//                String url = (baseUrl + reqMapping.value()).replaceAll("/+", "/");
//                handlerMapping.put(url, method);
//                System.out.println("Mapping " + url + "," + method);
//            }
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                //没有加requestMapping注解的直接忽略
                if (!method.isAnnotationPresent(SWRequestMapping.class)) {
                    continue;
                }
                //映射url
                SWRequestMapping requestMapping = method.getAnnotation(SWRequestMapping.class);
                String regex = ("/" + baseUrl + requestMapping.value()).replaceAll("/+", "/");
                Pattern pattern = Pattern.compile(regex);
                handlerMapping.add(new Handler(pattern, entry.getValue(), method));
                System.out.println("mapping " + regex + "," + method);
            }

        }
    }

    private void doAutowrited() {
        if (ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //首先第一步要获取到所有的字段 fields
            //不管是private还是protected还是default都要强制注入
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                // 判断是否有注解 不是就 continue
                if (!field.isAnnotationPresent(SWAutowrited.class)) {
                    continue;
                }
                SWAutowrited aot = field.getAnnotation(SWAutowrited.class);
                String beanName = aot.value().trim();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }

                // 要想访问到私有的，或者受保护的，我们强制授权访问
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private void doInstance() {

        if (classNames.isEmpty()) {
            return;
        }

        // 如果不为空，利用反射机制将刚刚扫描进来的所有的className初始化
        try {
            for (String className : classNames) {

                Class clazz = Class.forName(className);
                //接下来进入Bean的实例化阶段，初始化IOC容器
                //使用isAnnotationPresent来判断是否被@SWController注解所修饰，
                //因为我们只扫描被注解修饰的类加入到IOC容器中

                /**
                 * IOC容器规则
                 * 1. key默认用类名首字母小写
                 */
                if (clazz.isAnnotationPresent(SWController.class)) {
                    String beanName = lowerFisrtCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                } else if (clazz.isAnnotationPresent(SWService.class)) {
                    //2. 如果用户自定义名字，就优先选择自定义的名字
                    SWService service = (SWService) clazz.getAnnotation(SWService.class);
                    String beanName = service.value();
                    if ("".equals(beanName.trim())) {
                        beanName = lowerFisrtCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    //3. 如果是接口的话，我们可以巧妙用接口类型作为key
                    Class[] interfaces = clazz.getInterfaces();
                    for (Class i : interfaces) {
                        // 将接口的类型作为key
                        ioc.put(i.getName(), instance);
                    }
                } else {
                    continue;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String lowerFisrtCase(String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String packageName) {
        //进行递归扫描 不懂getResource() 方法的自己可以在main方法中跑一跑就明白了 获取的是项目主目录，不是类的根目录
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File classDir = new File(url.getFile());
        for (File file : classDir.listFiles()) {
            if (file.isDirectory()) {
                doScanner(packageName + "." + file.getName());
            } else {

                classNames.add(packageName + "." + file.getName().replace(".class", ""));

            }
        }
    }

    private void doLoadConfig(String location) {
        // Class.getResourceAsStream(String path) ： path 不以’/'开头时默认是从此类所在的包下取资源，以’/'开头则是从ClassPath根下获取。其只是通过path构造一个绝对路径，最终还是由ClassLoader获取资源。
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            p.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    /**
     * 运行时阶段执行的方法
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //6. 等待请求，进入运行阶段
//        String defaultContextPath = "/demo_war";
//        String url = req.getRequestURI();
//        String contextPath = req.getContextPath();
//        System.out.println("===========url: " + url);
//        System.out.println("===========contextPath: " + contextPath);
//        url = url.replace(contextPath, "").replaceAll("/+", "/").replaceAll(defaultContextPath, "");
//        System.out.println("===========repUrl: " + url);
//        if (!handlerMapping.containsKey(url)) {
//            resp.getWriter().write("404 Not Found!!!");
//            return;
//        }
//        System.out.println("===========OKurl: " + url);
//        Method m = handlerMapping.get(url);
//        // 反射的方法
//        // 需要两个参数，第一个拿到这个method的instance，第二个参数，要拿到实参，从request中取值
//        m.invoke()
//        System.out.println("=============" + m);

        try {

            doDispatch(req, resp); //开始匹配到对应的方法

        } catch (Exception e) {
            // 如果匹配过程出现异常，将异常信息打印出去
            resp.getWriter().write("500 Exception, Details:\r\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            Handler handler = getHandler(request);

            if (handler == null) {
                //如果没有匹配上，返回404错误
                response.getWriter().write("404 Not Found");
                return;
            }

            // 获取方法的参数列表
            Class[] paramTypes = handler.method.getParameterTypes();
            // 保存所有需要自动赋值的参数值
            Object[] paramValues = new Object[paramTypes.length];

            //这是属于J2EE中的内容
            Map<String, String[]> params = request.getParameterMap();
            for (Map.Entry<String, String[]> param : params.entrySet()) {
                System.out.println("param.getValue()" + param.getValue());
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "");
                // 如果找到匹配的对象，则开始填充参数值
                if (!handler.paramIndexMapping.containsKey(param.getKey())) {
                    continue;
                }
                int index = handler.paramIndexMapping.get(param.getKey());
                paramValues[index] = convert(paramTypes[index], value);
            }

            // 设置方法中的request和response对象
            int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = request;
            int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = response;

            handler.method.invoke(handler.controller, paramValues);

        } catch (Exception e) {
            throw e;
        }
    }

    private Handler getHandler(HttpServletRequest req) throws Exception {
        if (handlerMapping.isEmpty()) {
            return null;
        }
        String defaultContextPath = "/demo_war";
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/").replaceAll(defaultContextPath, "");
        for (Handler handler : handlerMapping) {
            try {
                Matcher matcher = handler.pattern.matcher(url);
                // 如果没有匹配到，继续下一个匹配
                if (!matcher.matches()) {
                    continue;
                }
                return handler;
            } catch (Exception e) {
                throw e;
            }
        }
        return null;
    }


    private Object convert(Class type, String value) {
        if (Integer.class == type) {
            return Integer.valueOf(value);
        }
        return value;
    }

    private class Handler {
        protected Object controller; // 保存方法对应的实例
        protected Method method; // 保存映射的方法
        protected Pattern pattern;
        protected Map<String, Integer> paramIndexMapping; //参数顺序

        protected Handler(Pattern pattern, Object controller, Method method) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;

            paramIndexMapping = new HashMap<String, Integer>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method) {
            Annotation[][] pa = method.getParameterAnnotations();
            for (int i = 0; i < pa.length; i++) {
                for (Annotation a : pa[i]) {
                    if (a instanceof SWRequestParam) {
                        String paramName = ((SWRequestParam) a).value();
                        if (!"".equals(paramName.trim())) {
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }

            // 提取方法中的request和response参数
            Class[] paramsTypes = method.getParameterTypes();
            for (int i = 0; i < paramsTypes.length; i++) {
                Class type = paramsTypes[i];
                if (type == HttpServletRequest.class || type == HttpServletResponse.class) {
                    paramIndexMapping.put(type.getName(), i);
                }
            }
        }
    }


}
