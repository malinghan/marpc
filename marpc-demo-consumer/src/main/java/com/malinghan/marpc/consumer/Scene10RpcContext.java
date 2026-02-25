package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.context.RpcContext;
import com.malinghan.marpc.demo.HelloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 场景10：RpcContext 隐式传参
 * - Consumer 设置上下文参数（grayId、traceId 等）
 * - Provider 读取上下文参数
 * - 验证参数透传成功
 */
@Slf4j
@Component
public class Scene10RpcContext {

    @MarpcConsumer
    private HelloService helloService;

    public void run() {
        log.info("=== Scene10: RpcContext 隐式传参 ===");
        log.info("  说明：通过 RpcContext 在调用链中透传上下文参数，无需修改接口签名");

        // 测试1：设置 grayId
        log.info("\n  [测试1] 设置 grayId");
        RpcContext.setGrayId("gray-user-001");
        log.info("    Consumer 设置: grayId=gray-user-001");
        try {
            String result = helloService.hello("context-test-1");
            log.info("    调用成功: {}", result);
            log.info("    提示：Provider 可通过 RpcContext.getGrayId() 读取到 'gray-user-001'");
        } catch (Exception e) {
            log.error("    调用失败: {}", e.getMessage());
        }

        // 测试2：设置多个参数
        log.info("\n  [测试2] 设置多个上下文参数");
        RpcContext.set("traceId", "trace-abc-123");
        RpcContext.set("userId", "user-456");
        RpcContext.setGrayId("gray-user-002");
        log.info("    Consumer 设置: traceId=trace-abc-123, userId=user-456, grayId=gray-user-002");
        try {
            String result = helloService.hello("context-test-2");
            log.info("    调用成功: {}", result);
            log.info("    提示：Provider 可通过 RpcContext.get(key) 读取所有参数");
        } catch (Exception e) {
            log.error("    调用失败: {}", e.getMessage());
        }

        // 测试3：验证 ThreadLocal 自动清理
        log.info("\n  [测试3] 验证 ThreadLocal 自动清理");
        log.info("    第一次调用前设置 grayId=gray-user-003");
        RpcContext.setGrayId("gray-user-003");
        try {
            helloService.hello("context-test-3");
            log.info("    第一次调用完成");
        } catch (Exception e) {
            log.error("    第一次调用失败: {}", e.getMessage());
        }

        log.info("    第二次调用前不设置任何参数");
        try {
            String result = helloService.hello("context-test-4");
            log.info("    第二次调用成功: {}", result);
            log.info("    提示：Provider 应读取不到上次的 grayId（已自动清理）");
        } catch (Exception e) {
            log.error("    第二次调用失败: {}", e.getMessage());
        }

        log.info("=== Scene10 完成 ===\n");
    }
}
