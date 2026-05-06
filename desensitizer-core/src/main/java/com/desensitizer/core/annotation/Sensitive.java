package com.desensitizer.core.annotation;

import com.desensitizer.core.api.Desensitizer;
import com.desensitizer.core.api.SensitiveType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {
    SensitiveType type() default SensitiveType.CUSTOM;
    Class<? extends Desensitizer> customType() default NoOpDesensitizer.class;
    String pattern() default "";
    int keepLeft() default 0;
    int keepRight() default 0;
}
