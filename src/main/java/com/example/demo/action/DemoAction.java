package com.example.demo.action;

import com.example.demo.service.impl.IDemoService;
import com.example.mvcframework.annotation.SWAutowrited;
import com.example.mvcframework.annotation.SWController;
import com.example.mvcframework.annotation.SWRequestMapping;
import com.example.mvcframework.annotation.SWRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author SW
 * @date create 2019-04-24 12:41
 */
@SWController
@SWRequestMapping("/demo")
public class DemoAction {

    @SWAutowrited
    private IDemoService demoService;

    @SWRequestMapping("/query.json")
    public void query(HttpServletRequest req, HttpServletResponse resp, @SWRequestParam("name") String name) {
        String result = demoService.get(name);
        try {
            resp.getWriter().write(name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
