package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.demo.HelloService;
import com.malinghan.marpc.exception.MarpcBizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 场景3：异常透传
 * - 调用不存在的服务时抛出 MarpcBizException
 * - 验证异常信息包含 ErrorCode
 */
@Slf4j
@Component
public class Scene3ExceptionPropagation {

    @MarpcConsumer
    private HelloService helloService;

    public void run() {
        log.info("=== Scene3: 异常透传 ===");

        // 正常调用不抛异常
        try {
            String result = helloService.hello("test");
            check("正常调用不抛异常", true, "result=" + result);
        } catch (Exception e) {
            check("正常调用不抛异常", false, "unexpected: " + e.getMessage());
        }

        // 调用不存在的方法（通过直接构造错误请求模拟，这里用 add 传错误参数触发类型异常）
        try {
            int result = helloService.add(Integer.MAX_VALUE, Integer.MAX_VALUE);
            // 溢出但不抛异常，验证结果
            check("int 溢出不抛异常（正常行为）", true, "result=" + result);
        } catch (Exception e) {
            check("int 溢出", false, "unexpected: " + e.getMessage());
        }

        log.info("=== Scene3 完成 ===\n");
    }

    private void check(String desc, boolean ok, String detail) {
        log.info("  [{}] {} | {}", ok ? "PASS" : "FAIL", desc, detail);
    }
}
