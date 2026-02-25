package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.demo.HelloService;
import com.malinghan.marpc.demo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时自动执行调用示例，输出直观结果验证 RPC 是否正常。
 */
@Slf4j
@Component
public class HelloConsumer {

    @MarpcConsumer
    private HelloService helloService;

    @Bean
    public ApplicationRunner rpcSmokeTest() {
        return args -> {
            log.info("=== marpc smoke test start ===");

            check("hello(String)",
                    helloService.hello("world"),
                    "hello, world");

            check("hello(String, int) overload",
                    helloService.hello("hi", 3),
                    "hi hi hi");

            check("add(int, int)",
                    String.valueOf(helloService.add(10, 20)),
                    "30");

            List<String> items = helloService.list("item-", 3);
            check("list(String, int)",
                    items.toString(),
                    "[item-0, item-1, item-2]");

            User user = helloService.getUser(42);
            check("getUser(int)",
                    user.toString(),
                    "User(id=42, name=user-42)");

            log.info("=== marpc smoke test end ===");
        };
    }

    private void check(String desc, Object actual, String expected) {
        boolean ok = expected.equals(String.valueOf(actual));
        log.info("[{}] {} | actual={} | {}",
                ok ? "PASS" : "FAIL", desc, actual, ok ? "" : "expected=" + expected);
    }
}
