package com.malinghan.marpc.annotation;

import java.lang.annotation.*;

/**
 * 标记服务提供方实现类。
 *
 * <p>标注此注解的 Bean 会在 {@link com.malinghan.marpc.provider.ProviderBootstrap} 启动时
 * 被扫描，其实现的所有接口将以接口全限定名为 key 注册到 skeleton map，对外暴露为 RPC 服务。
 *
 * <pre>{@code
 * @Service
 * @MarpcProvider
 * public class HelloServiceImpl implements HelloService { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MarpcProvider {
}