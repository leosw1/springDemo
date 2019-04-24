package com.example.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author SW
 * @date create 2019-04-24 12:49
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SWRequestParam {

    String value() default "";
}
