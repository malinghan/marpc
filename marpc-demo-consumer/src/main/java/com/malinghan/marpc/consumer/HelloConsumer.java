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

    public HelloConsumer(Scene1BasicAndOverload scene1,
                         Scene2ComplexTypes scene2,
                         Scene3ExceptionPropagation scene3,
                         Scene4ConcurrentCalls scene4) {
        this.scene1 = scene1;
        this.scene2 = scene2;
        this.scene3 = scene3;
        this.scene4 = scene4;
    }

    @Bean
    public ApplicationRunner rpcSmokeTest() {
        return args -> {
            log.info("\n\n========== marpc v3.0 smoke test start ==========\n");

            scene1.run();
            scene2.run();
            scene3.run();
            scene4.run();

            log.info("========== marpc v3.0 smoke test end ==========\n");
        };
    }
}
