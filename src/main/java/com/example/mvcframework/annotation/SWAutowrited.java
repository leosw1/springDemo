package com.example.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author SW
 * @date create 2019-04-24 12:48
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SWAutowrited {

    String value() default "";
}
