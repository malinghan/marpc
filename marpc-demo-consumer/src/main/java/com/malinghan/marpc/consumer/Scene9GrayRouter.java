package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.demo.HelloService;
import com.malinghan.marpc.router.GrayRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 场景9：灰度路由
 * - 标记灰度实例
 * - 配置灰度比例（grayRatio）
 * - 验证流量按比例路由到灰度/正常实例
 */
@Slf4j
@Component
public class Scene9GrayRouter {

    @MarpcConsumer
    private HelloService helloService;

    private final GrayRouter grayRouter;

    public Scene9GrayRouter(GrayRouter grayRouter) {
        this.grayRouter = grayRouter;
    }

    public void run() {
        log.info("=== Scene9: 灰度路由 ===");
        log.info("  提示：需要启用灰度路由配置（marpc.router.gray.enabled=true）");
        log.info("  配置：grayRatio=50（50% 流量路由到灰度实例）");

        // 标记灰度实例（假设 localhost:8082 是灰度节点）
        grayRouter.markGray("localhost:8082");
        log.info("  已标记灰度实例: localhost:8082");

        // 多次调用，观察路由分布
        log.info("  执行 10 次调用，观察灰度/正常实例分布:");
        for (int i = 0; i < 10; i++) {
            try {
                String result = helloService.hello("gray-test-" + i);
                log.info("    第 {} 次调用成功: {}", i + 1, result);
            } catch (Exception e) {
                log.error("    第 {} 次调用失败: {}", i + 1, e.getMessage());
            }
        }

        // 清除灰度标记
        grayRouter.clearGray();
        log.info("  已清除灰度标记");

        log.info("=== Scene9 完成 ===\n");
    }
}
