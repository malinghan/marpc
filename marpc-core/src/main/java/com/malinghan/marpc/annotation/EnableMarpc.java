package com.malinghan.marpc.annotation;

import com.malinghan.marpc.config.MarpcConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MarpcConfig.class)
public @interface EnableMarpc {
}
