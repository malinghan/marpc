package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.demo.HelloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 场景1：基本调用 & 方法重载
 * - hello(String) 基本调用
 * - hello(String, int) 重载区分
 * - add(int, int) 基本类型
 * - list(String, int) List 返回
 */
@Slf4j
@Component
public class Scene1BasicAndOverload {

    @MarpcConsumer
    private HelloService helloService;

    public void run() {
        log.info("=== Scene1: 基本调用 & 方法重载 ===");

        check("hello(String)",
                helloService.hello("world"),
                "hello, world");

        check("hello(String,int) 重载",
                helloService.hello("hi", 3),
                "hi hi hi");

        check("add(int,int)",
                String.valueOf(helloService.add(10, 20)),
                "30");

        check("list(String,int)",
                helloService.list("item-", 3).toString(),
                "[item-0, item-1, item-2]");

        log.info("=== Scene1 完成 ===\n");
    }

    private void check(String desc, Object actual, String expected) {
        boolean ok = expected.equals(String.valueOf(actual));
        log.info("  [{}] {} => {}", ok ? "PASS" : "FAIL", desc, actual);
    }
}
