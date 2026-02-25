package com.malinghan.marpc.annotation;

import com.malinghan.marpc.config.MarpcConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 marpc 框架的入口注解，标注在 Spring Boot 启动类上。
 *
 * <p>通过 {@code @Import(MarpcConfig.class)} 将框架所有核心 Bean 引入容器，
 * 用户无需手动注册任何配置，加上此注解即可激活 Provider 注册、Consumer 注入和 HTTP 传输层。
 *
 * <pre>{@code
 * @SpringBootApplication
 * @EnableMarpc
 * public class MyApplication { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MarpcConfig.class)
public @interface EnableMarpc {
}
