package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.demo.HelloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 场景7：重试机制
 * - 模拟调用失败（关闭 Provider），观察重试日志
 * - 验证重试次数、超时、切换实例
 */
@Slf4j
@Component
public class Scene7Retry {

    @MarpcConsumer
    private HelloService helloService;

    public void run() {
        log.info("=== Scene7: 重试机制 ===");
        log.info("  提示：需要手动关闭 Provider 或模拟网络故障来观察重试效果");
        log.info("  配置：maxRetries=2, timeout=3000ms, switchInstanceOnRetry=true");

        try {
            String result = helloService.hello("retry-test");
            log.info("  [SUCCESS] 调用成功: {}", result);
        } catch (Exception e) {
            log.error("  [FAIL] 调用失败: {}", e.getMessage());
            log.info("  观察日志中的 [Retry] 标记，验证重试次数和实例切换");
        }

        log.info("=== Scene7 完成 ===\n");
    }
}
