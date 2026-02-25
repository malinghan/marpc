package com.malinghan.marpc.filter;

import com.malinghan.marpc.core.RpcRequest;
import com.malinghan.marpc.core.RpcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Filter 链单元测试：验证 CacheFilter、MockFilter、执行顺序、短路逻辑。
 */
class FilterChainTest {

    private CacheFilter cacheFilter;
    private MockFilter mockFilter;

    @BeforeEach
    void setUp() {
        cacheFilter = new CacheFilter();
        mockFilter = new MockFilter();
    }

    // ---- CacheFilter ----

    @Test
    void cache_miss_returnsNull() {
        RpcRequest req = req("hello@1_java.lang.String", "world");
        assertNull(cacheFilter.preFilter(req));
    }

    @Test
    void cache_hit_returnsShortCircuit() {
        RpcRequest req = req("hello@1_java.lang.String", "world");
        RpcResponse resp = RpcResponse.ok("hello, world");

        // 第一次 preFilter miss，postFilter 写入缓存
        assertNull(cacheFilter.preFilter(req));
        cacheFilter.postFilter(req, resp);

        // 第二次 preFilter 命中
        RpcResponse cached = cacheFilter.preFilter(req);
        assertNotNull(cached);
        assertEquals("hello, world", cached.getData());
        assertEquals(1, cacheFilter.size());
    }

    @Test
    void cache_failedResponse_notCached() {
        RpcRequest req = req("hello@1_java.lang.String", "world");
        RpcResponse errResp = RpcResponse.error("service not found");

        cacheFilter.postFilter(req, errResp);

        // 失败响应不写缓存
        assertNull(cacheFilter.preFilter(req));
        assertEquals(0, cacheFilter.size());
    }

    @Test
    void cache_differentArgs_separateEntries() {
        RpcRequest req1 = req("hello@1_java.lang.String", "Alice");
        RpcRequest req2 = req("hello@1_java.lang.String", "Bob");

        cacheFilter.postFilter(req1, RpcResponse.ok("hello, Alice"));
        cacheFilter.postFilter(req2, RpcResponse.ok("hello, Bob"));

        assertEquals("hello, Alice", cacheFilter.preFilter(req1).getData());
        assertEquals("hello, Bob", cacheFilter.preFilter(req2).getData());
        assertEquals(2, cacheFilter.size());
    }

    @Test
    void cache_clear_removesAllEntries() {
        RpcRequest req = req("hello@1_java.lang.String", "world");
        cacheFilter.postFilter(req, RpcResponse.ok("hello, world"));
        assertEquals(1, cacheFilter.size());

        cacheFilter.clear();
        assertEquals(0, cacheFilter.size());
        assertNull(cacheFilter.preFilter(req));
    }

    // ---- MockFilter ----

    @Test
    void mock_matchingSign_returnsShortCircuit() {
        mockFilter.mock("hello@1_java.lang.String", "mocked!");
        RpcRequest req = req("hello@1_java.lang.String", "world");

        RpcResponse resp = mockFilter.preFilter(req);
        assertNotNull(resp);
        assertTrue(resp.isStatus());
        assertEquals("mocked!", resp.getData());
    }

    @Test
    void mock_noMatchingSign_returnsNull() {
        mockFilter.mock("add@2_int_int", 99);
        RpcRequest req = req("hello@1_java.lang.String", "world");

        assertNull(mockFilter.preFilter(req));
    }

    @Test
    void mock_clearMocks_noLongerMatches() {
        mockFilter.mock("hello@1_java.lang.String", "mocked!");
        mockFilter.clearMocks();

        RpcRequest req = req("hello@1_java.lang.String", "world");
        assertNull(mockFilter.preFilter(req));
    }

    // ---- 执行顺序 ----

    @Test
    void order_mockBeforeCache() {
        // MockFilter order=0, CacheFilter order=10
        List<Filter> sorted = List.of(cacheFilter, mockFilter).stream()
                .sorted((a, b) -> Integer.compare(a.order(), b.order()))
                .toList();
        assertInstanceOf(MockFilter.class, sorted.get(0));
        assertInstanceOf(CacheFilter.class, sorted.get(1));
    }

    // ---- 短路逻辑 ----

    @Test
    void shortCircuit_mockPreventsRemoteCall() {
        // 模拟 Filter 链：MockFilter 命中后不应继续执行
        mockFilter.mock("hello@1_java.lang.String", "mock-value");
        RpcRequest req = req("hello@1_java.lang.String", "world");

        // preFilter 返回非 null 即短路
        RpcResponse result = mockFilter.preFilter(req);
        assertNotNull(result);
        assertEquals("mock-value", result.getData());
    }

    // ---- 工具方法 ----

    private RpcRequest req(String sign, Object... args) {
        RpcRequest r = new RpcRequest();
        r.setService("com.malinghan.marpc.demo.HelloService");
        r.setMethod("hello");
        r.setMethodSign(sign);
        r.setArgs(args);
        return r;
    }
}
