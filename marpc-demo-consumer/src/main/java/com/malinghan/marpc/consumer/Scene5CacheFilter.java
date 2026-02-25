package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.demo.HelloService;
import com.malinghan.marpc.filter.CacheFilter;
import com.malinghan.marpc.filter.MockFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 场景5：CacheFilter 缓存效果
 * - 第一次调用发起远程请求，结果写入缓存
 * - 第二次相同调用命中缓存，不发网络请求（通过日志 [CacheFilter] 命中缓存 验证）
 */
@Slf4j
@Component
public class Scene5CacheFilter {

    @MarpcConsumer
    private HelloService helloService;

    private final CacheFilter cacheFilter;

    public Scene5CacheFilter(CacheFilter cacheFilter) {
        this.cacheFilter = cacheFilter;
    }

    public void run() {
        log.info("=== Scene5: CacheFilter 缓存效果 ===");
        log.info("  注意观察日志：第二次调用应出现 [CacheFilter] 命中缓存");

        cacheFilter.clear();

        // 第一次调用：缓存 miss，发起远程调用
        String r1 = helloService.hello("cache-test");
        check("第一次调用（缓存 miss）", r1, "hello, cache-test");
        log.info("  缓存条目数: {}", cacheFilter.size());

        // 第二次调用：缓存 hit，不发网络请求
        String r2 = helloService.hello("cache-test");
        check("第二次调用（缓存 hit）", r2, "hello, cache-test");

        // 不同参数：缓存 miss
        String r3 = helloService.hello("other");
        check("不同参数（缓存 miss）", r3, "hello, other");
        log.info("  最终缓存条目数: {}", cacheFilter.size());

        log.info("=== Scene5 完成 ===\n");
    }

    private void check(String desc, Object actual, String expected) {
        boolean ok = expected.equals(String.valueOf(actual));
        log.info("  [{}] {} => {}", ok ? "PASS" : "FAIL", desc, actual);
    }
}
