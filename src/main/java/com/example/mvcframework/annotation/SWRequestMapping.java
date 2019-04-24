package com.example.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author SW
 * @date create 2019-04-24 12:47
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SWRequestMapping {

    String value() default "";
}
