package com.example.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author SW
 * @date create 2019-04-24 12:44
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SWController {

    String value() default "";
}
