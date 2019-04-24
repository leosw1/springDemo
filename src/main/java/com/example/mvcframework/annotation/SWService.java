package com.example.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author SW
 * @date create 2019-04-24 12:50
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SWService {

    String value() default "";
}
