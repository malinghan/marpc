package com.malinghan.marpc.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Consumer 启动时自动执行所有测试场景，输出直观结果验证 RPC 功能。
 */
@Slf4j
@Component
public class HelloConsumer {

    private final Scene1BasicAndOverload scene1;
    private final Scene2ComplexTypes scene2;
    private final Scene3ExceptionPropagation scene3;
    private final Scene4ConcurrentCalls scene4;
    private final Scene5CacheFilter scene5;
    private final Scene6MockFilter scene6;
    private final Scene7Retry scene7;
    private final Scene8CircuitBreaker scene8;
    private final Scene9GrayRouter scene9;

    public HelloConsumer(Scene1BasicAndOverload scene1,
                         Scene2ComplexTypes scene2,
                         Scene3ExceptionPropagation scene3,
                         Scene4ConcurrentCalls scene4,
                         Scene5CacheFilter scene5,
                         Scene6MockFilter scene6,
                         Scene7Retry scene7,
                         Scene8CircuitBreaker scene8,
                         Scene9GrayRouter scene9) {
        this.scene1 = scene1;
        this.scene2 = scene2;
        this.scene3 = scene3;
        this.scene4 = scene4;
        this.scene5 = scene5;
        this.scene6 = scene6;
        this.scene7 = scene7;
        this.scene8 = scene8;
        this.scene9 = scene9;
    }

    @Bean
    public ApplicationRunner rpcSmokeTest() {
        return args -> {
            log.info("\n\n========== marpc v5.0 smoke test start ==========\n");

            scene1.run();
            scene2.run();
            scene3.run();
            scene4.run();
            scene5.run();
            scene6.run();
            scene7.run();
            scene8.run();
            scene9.run();

            log.info("========== marpc v5.0 smoke test end ==========\n");
        };
    }
}
