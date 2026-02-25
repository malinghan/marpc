package com.malinghan.marpc.provider;

import com.malinghan.marpc.annotation.MarpcProvider;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import com.malinghan.marpc.registry.RegistryCenter;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProviderBootstrapTest {

    interface HelloService {
        String hello(String name);
        int add(int a, int b);
    }

    @MarpcProvider
    static class HelloServiceImpl implements HelloService {
        @Override
        public String hello(String name) {
            return "hello, " + name;
        }

        @Override
        public int add(int a, int b) {
            return a + b;
        }
    }

    private ProviderBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        HelloServiceImpl impl = new HelloServiceImpl();
        when(ctx.getBeansWithAnnotation(MarpcProvider.class)).thenReturn(Map.of("helloService", impl));

        RegistryCenter registry = mock(RegistryCenter.class);
        bootstrap = new ProviderBootstrap(ctx, registry, "localhost:8080");
        bootstrap.start();
    }

    @Test
    void invoke_returnsCorrectResult() {
        RpcRequest req = new RpcRequest();
        req.setService(HelloService.class.getCanonicalName());
        req.setMethod("hello");
        req.setArgs(new Object[]{"world"});

        RpcResponse resp = bootstrap.invoke(req);

        assertTrue(resp.isStatus());
        assertEquals("hello, world", resp.getData());
    }

    @Test
    void invoke_multipleArgs() {
        RpcRequest req = new RpcRequest();
        req.setService(HelloService.class.getCanonicalName());
        req.setMethod("add");
        req.setArgs(new Object[]{3, 4});

        RpcResponse resp = bootstrap.invoke(req);

        assertTrue(resp.isStatus());
        assertEquals(7, resp.getData());
    }

    @Test
    void invoke_serviceNotFound_returnsError() {
        RpcRequest req = new RpcRequest();
        req.setService("com.example.NonExistentService");
        req.setMethod("foo");
        req.setArgs(null);

        RpcResponse resp = bootstrap.invoke(req);

        assertFalse(resp.isStatus());
        assertTrue(resp.getErrorMessage().contains("service not found"));
    }

    @Test
    void invoke_methodNotFound_returnsError() {
        RpcRequest req = new RpcRequest();
        req.setService(HelloService.class.getCanonicalName());
        req.setMethod("nonExistentMethod");
        req.setArgs(new Object[]{});

        RpcResponse resp = bootstrap.invoke(req);

        assertFalse(resp.isStatus());
    }
}