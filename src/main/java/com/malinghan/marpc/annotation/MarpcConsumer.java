package com.malinghan.marpc.annotation;

import java.lang.annotation.*;

/**
 * 标记需要注入 RPC 代理的字段。
 *
 * <p>标注此注解的字段会在 {@link com.malinghan.marpc.consumer.ConsumerBootstrap} 启动时
 * 被扫描，框架自动为该字段的接口类型创建 JDK 动态代理并注入，调用方无需感知网络细节。
 *
 * <pre>{@code
 * @MarpcConsumer
 * private HelloService helloService;
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MarpcConsumer {
}