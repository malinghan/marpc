package com.malinghan.marpc.demo;

import com.malinghan.marpc.annotation.MarpcProvider;
import org.springframework.stereotype.Service;

/**
 * {@link HelloService} 的示例实现，同时作为 marpc Provider 注册到框架。
 *
 * <p>{@link com.malinghan.marpc.annotation.MarpcProvider} 使框架在启动时将此 Bean
 * 以 {@code HelloService} 的全限定名注册到 skeleton map，对外暴露为可远程调用的服务。
 */
@Service
@MarpcProvider
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "hello, " + name;
    }
}