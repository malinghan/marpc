package com.malinghan.marpc.provider;

import com.malinghan.marpc.annotation.MarpcProvider;
import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import com.malinghan.marpc.demo.User;
import com.malinghan.marpc.registry.RegistryCenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * v2.0 集成测试：验证方法签名匹配、参数类型转换、重载区分、复杂返回类型。
 * 不依赖 Spring 容器，直接构造 ProviderBootstrap 测试核心逻辑。
 */
class ProviderBootstrapV2Test {

    interface HelloService {
        String hello(String name);
        String hello(String name, int times);
        int add(int a, int b);
        List<String> list(String prefix, int count);
        User getUser(int id);
    }

    @MarpcProvider
    static class HelloServiceImpl implements HelloService {
        @Override public String hello(String name) { return "hello, " + name; }
        @Override public String hello(String name, int times) { return (name + " ").repeat(times).trim(); }
        @Override public int add(int a, int b) { return a + b; }
        @Override public List<String> list(String prefix, int count) {
            return java.util.stream.IntStream.range(0, count)
                    .mapToObj(i -> prefix + i)
                    .collect(java.util.stream.Collectors.toList());
        }
        @Override public User getUser(int id) { return new User(id, "user-" + id); }
    }

    private ProviderBootstrap bootstrap;

    @BeforeEach
    void setUp() {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBeansWithAnnotation(MarpcProvider.class))
                .thenReturn(Map.of("helloService", new HelloServiceImpl()));
        RegistryCenter registry = mock(RegistryCenter.class);
        bootstrap = new ProviderBootstrap(ctx, registry, "localhost:8080");
        bootstrap.start();
    }

    // ---- 方法签名匹配 & 重载区分 ----

    @Test
    void hello_oneArg_matchedBySign() {
        RpcResponse resp = bootstrap.invoke(req("hello@1_java.lang.String", "hello", "world"));
        assertTrue(resp.isStatus());
        assertEquals("hello, world", resp.getData());
    }

    @Test
    void hello_twoArgs_overloadDistinguished() {
        RpcResponse resp = bootstrap.invoke(req("hello@2_java.lang.String_int", "hello", "hi", 3));
        assertTrue(resp.isStatus());
        assertEquals("hi hi hi", resp.getData());
    }

    // ---- 基本类型转换 ----

    @Test
    void add_intArgs_typeConvertedCorrectly() {
        // JSON 反序列化后 int 可能变成 Integer/Long，convertArgs 应正确转换
        RpcResponse resp = bootstrap.invoke(req("add@2_int_int", "add", 10, 20));
        assertTrue(resp.isStatus());
        assertEquals(30, ((Number) resp.getData()).intValue());
    }

    // ---- List 返回类型 ----

    @Test
    void list_returnsCorrectList() {
        RpcResponse resp = bootstrap.invoke(req("list@2_java.lang.String_int", "list", "item-", 3));
        assertTrue(resp.isStatus());
        @SuppressWarnings("unchecked")
        List<String> data = (List<String>) resp.getData();
        assertEquals(List.of("item-0", "item-1", "item-2"), data);
    }

    // ---- 自定义对象返回类型 ----

    @Test
    void getUser_returnsUserObject() {
        RpcResponse resp = bootstrap.invoke(req("getUser@1_int", "getUser", 42));
        assertTrue(resp.isStatus());
        // data 是 JSONObject，需要用 JSON.to 转换（Consumer 侧做，这里验证 data 不为 null）
        assertNotNull(resp.getData());
    }

    // ---- 错误场景 ----

    @Test
    void serviceNotFound_returnsError() {
        RpcRequest r = new RpcRequest();
        r.setService("com.example.NoSuchService");
        r.setMethod("foo");
        r.setMethodSign("foo@0");
        r.setArgs(null);
        RpcResponse resp = bootstrap.invoke(r);
        assertFalse(resp.isStatus());
        assertTrue(resp.getErrorMessage().contains("service not found"));
    }

    @Test
    void methodNotFound_returnsError() {
        RpcResponse resp = bootstrap.invoke(req("noSuchMethod@0", "noSuchMethod"));
        assertFalse(resp.isStatus());
    }

    // ---- 负载均衡单元测试 ----

    @Test
    void roundRobin_distributeEvenly() {
        var lb = new com.malinghan.marpc.loadbalance.RoundRobinLoadBalancer();
        List<String> instances = List.of("a", "b", "c");
        assertEquals("a", lb.choose(instances));
        assertEquals("b", lb.choose(instances));
        assertEquals("c", lb.choose(instances));
        assertEquals("a", lb.choose(instances)); // 轮回
    }

    @Test
    void random_alwaysReturnsValidInstance() {
        var lb = new com.malinghan.marpc.loadbalance.RandomLoadBalancer();
        List<String> instances = List.of("x", "y", "z");
        for (int i = 0; i < 20; i++) {
            assertTrue(instances.contains(lb.choose(instances)));
        }
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
