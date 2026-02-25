package com.malinghan.marpc.demo;

/**
 * 示例服务接口，用于演示 marpc 的基本用法。
 *
 * <p>Provider 侧由 {@link HelloServiceImpl} 实现并标注 {@link com.malinghan.marpc.annotation.MarpcProvider}，
 * Consumer 侧通过 {@link com.malinghan.marpc.annotation.MarpcConsumer} 注入代理后直接调用。
 */
public interface HelloService {
    String hello(String name);
}