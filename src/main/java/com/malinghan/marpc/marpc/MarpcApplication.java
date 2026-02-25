package com.malinghan.marpc.marpc;

import com.malinghan.marpc.annotation.EnableMarpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * marpc 示例应用启动类。
 *
 * <p>{@link EnableMarpc} 激活框架，启动时自动完成 Provider 注册和 Consumer 代理注入。
 * 本示例将 Provider 和 Consumer 运行在同一进程中，仅用于演示目的；
 * 生产场景下应拆分为独立的 Provider 和 Consumer 应用。
 */
@SpringBootApplication
@EnableMarpc
public class MarpcApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarpcApplication.class, args);
    }

}
