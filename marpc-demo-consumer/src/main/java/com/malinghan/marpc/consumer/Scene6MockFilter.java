package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.demo.HelloService;
import com.malinghan.marpc.filter.MockFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 场景6：MockFilter 拦截返回
 * - 注册 mock 规则后，调用被拦截，直接返回 mock 值，不发网络请求
 * - 清除 mock 规则后，调用恢复正常
 */
@Slf4j
@Component
public class Scene6MockFilter {

    @MarpcConsumer
    private HelloService helloService;

    private final MockFilter mockFilter;

    public Scene6MockFilter(MockFilter mockFilter) {
        this.mockFilter = mockFilter;
    }

    public void run() {
        log.info("=== Scene6: MockFilter 拦截返回 ===");

        // 注册 mock 规则
        mockFilter.mock("hello@1_java.lang.String", "mocked-response");
        log.info("  已注册 mock: hello@1_java.lang.String -> mocked-response");

        String r1 = helloService.hello("world");
        check("mock 拦截（不发网络请求）", r1, "mocked-response");

        // 清除 mock，恢复正常调用
        mockFilter.clearMocks();
        log.info("  已清除 mock 规则");

        String r2 = helloService.hello("world");
        check("mock 清除后恢复正常调用", r2, "hello, world");

        log.info("=== Scene6 完成 ===\n");
    }

    private void check(String desc, Object actual, String expected) {
        boolean ok = expected.equals(String.valueOf(actual));
        log.info("  [{}] {} => {}", ok ? "PASS" : "FAIL", desc, actual);
    }
}
