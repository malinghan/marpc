package com.malinghan.marpc;

import com.malinghan.marpc.annotation.MarpcProvider;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import com.malinghan.marpc.exception.MarpcBizException;
import com.malinghan.marpc.exception.MarpcException;
import com.malinghan.marpc.loadbalance.RandomLoadBalancer;
import com.malinghan.marpc.loadbalance.RoundRobinLoadBalancer;
import com.malinghan.marpc.provider.ProviderBootstrap;
import com.malinghan.marpc.registry.RegistryCenter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarpcCoreV3Test {

    interface HelloService {
        String hello(String name);
        String hello(String name, int times);
        int add(int a, int b);
        List<String> list(String prefix, int count);
        User getUser(int id);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class User {
        private int id;
        private String name;
    }

    @MarpcProvider
    static class HelloServiceImpl implements HelloService {
        @Override public String hello(String name) { return "hello, " + name; }
        @Override public String hello(String name, int times) { return (name + " ").repeat(times).trim(); }
        @Override public int add(int a, int b) { return a + b; }
        @Override public List<String> list(String prefix, int count) {
            return IntStream.range(0, count).mapToObj(i -> prefix + i).collect(Collectors.toList());
        }
        @Override public User getUser(int id) { return new User(id, "user-" + id); }
    }

    private ProviderBootstrap bootstrap;

    @BeforeEach
    void setUp() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBeansWithAnnotation(MarpcProvider.class))
                .thenReturn(Map.of("helloService", new HelloServiceImpl()));
        RegistryCenter registry = mock(RegistryCenter.class);
        bootstrap = new ProviderBootstrap(ctx, registry, "localhost:8080");
        bootstrap.afterPropertiesSet();
    }

    // ---- 生命周期 ----

    @Test
    void lifecycle_afterPropertiesSet_registersService() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        RegistryCenter registry = mock(RegistryCenter.class);
        when(ctx.getBeansWithAnnotation(MarpcProvider.class))
                .thenReturn(Map.of("helloService", new HelloServiceImpl()));

        ProviderBootstrap pb = new ProviderBootstrap(ctx, registry, "localhost:9090");
        pb.afterPropertiesSet();

        verify(registry).register(HelloService.class.getCanonicalName(), "localhost:9090");
    }

    @Test
    void lifecycle_destroy_unregistersService() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        RegistryCenter registry = mock(RegistryCenter.class);
        when(ctx.getBeansWithAnnotation(MarpcProvider.class))
                .thenReturn(Map.of("helloService", new HelloServiceImpl()));

        ProviderBootstrap pb = new ProviderBootstrap(ctx, registry, "localhost:9090");
        pb.afterPropertiesSet();
        pb.destroy();

        verify(registry).unregister(HelloService.class.getCanonicalName(), "localhost:9090");
        verify(registry).stop();
    }

    // ---- 统一异常体系 ----

    @Test
    void exception_serviceNotFound_returnsErrorWithCode() {
        RpcRequest req = req("foo@0", "foo");
        req.setService("com.example.NoSuchService");

        RpcResponse resp = bootstrap.invoke(req);

        assertFalse(resp.isStatus());
        assertTrue(resp.getErrorMessage().contains("SERVICE_NOT_FOUND"));
    }

    @Test
    void exception_methodNotFound_returnsErrorWithCode() {
        RpcResponse resp = bootstrap.invoke(req("noSuchMethod@0", "noSuchMethod"));
        assertFalse(resp.isStatus());
        assertTrue(resp.getErrorMessage().contains("METHOD_NOT_FOUND"));
    }

    @Test
    void exception_hierarchy() {
        MarpcBizException biz = new MarpcBizException(
                MarpcException.ErrorCode.SERVICE_NOT_FOUND, "test");
        assertInstanceOf(MarpcException.class, biz);
        assertInstanceOf(RuntimeException.class, biz);
        assertEquals(MarpcException.ErrorCode.SERVICE_NOT_FOUND, biz.getErrorCode());
    }

    // ---- 方法签名 & 重载 ----

    @Test
    void sign_hello_oneArg() {
        RpcResponse resp = bootstrap.invoke(req("hello@1_java.lang.String", "hello", "world"));
        assertTrue(resp.isStatus());
        assertEquals("hello, world", resp.getData());
    }

    @Test
    void sign_hello_overload_twoArgs() {
        RpcResponse resp = bootstrap.invoke(req("hello@2_java.lang.String_int", "hello", "hi", 3));
        assertTrue(resp.isStatus());
        assertEquals("hi hi hi", resp.getData());
    }

    // ---- 类型转换 ----

    @Test
    void typeConvert_int() {
        RpcResponse resp = bootstrap.invoke(req("add@2_int_int", "add", 10, 20));
        assertTrue(resp.isStatus());
        assertEquals(30, ((Number) resp.getData()).intValue());
    }

    @Test
    void typeConvert_list() {
        RpcResponse resp = bootstrap.invoke(req("list@2_java.lang.String_int", "list", "x-", 3));
        assertTrue(resp.isStatus());
        assertEquals(List.of("x-0", "x-1", "x-2"), resp.getData());
    }

    @Test
    void typeConvert_customObject() {
        RpcResponse resp = bootstrap.invoke(req("getUser@1_int", "getUser", 7));
        assertTrue(resp.isStatus());
        assertNotNull(resp.getData());
    }

    // ---- 负载均衡 ----

    @Test
    void roundRobin_cycles() {
        var lb = new RoundRobinLoadBalancer();
        List<String> instances = List.of("a", "b", "c");
        assertEquals("a", lb.choose(instances));
        assertEquals("b", lb.choose(instances));
        assertEquals("c", lb.choose(instances));
        assertEquals("a", lb.choose(instances));
    }

    @Test
    void random_alwaysValid() {
        var lb = new RandomLoadBalancer();
        List<String> instances = List.of("x", "y", "z");
        for (int i = 0; i < 30; i++) assertTrue(instances.contains(lb.choose(instances)));
    }

    @Test
    void loadBalancer_emptyList_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoundRobinLoadBalancer().choose(List.of()));
    }

    // ---- 工具方法 ----

    private RpcRequest req(String sign, String method, Object... args) {
        RpcRequest r = new RpcRequest();
        r.setService(HelloService.class.getCanonicalName());
        r.setMethod(method);
        r.setMethodSign(sign);
        r.setArgs(args.length == 0 ? null : args);
        return r;
    }
}
