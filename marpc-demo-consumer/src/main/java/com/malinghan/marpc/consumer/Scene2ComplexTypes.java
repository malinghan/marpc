package com.malinghan.marpc.consumer;

import com.malinghan.marpc.annotation.MarpcConsumer;
import com.malinghan.marpc.demo.Order;
import com.malinghan.marpc.demo.OrderService;
import com.malinghan.marpc.demo.User;
import com.malinghan.marpc.demo.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 场景2：自定义对象 & 集合类型
 * - UserService.getUser(int) 返回自定义对象
 * - UserService.listUsers() 返回 List<User>
 * - OrderService.listByUser(int) 返回 List<Order>
 * - OrderService.createOrder(...) 创建并返回对象
 */
@Slf4j
@Component
public class Scene2ComplexTypes {

    @MarpcConsumer
    private UserService userService;

    @MarpcConsumer
    private OrderService orderService;

    public void run() {
        log.info("=== Scene2: 自定义对象 & 集合类型 ===");

        User user = userService.getUser(1);
        check("getUser(1)", user != null && "Alice".equals(user.getName()), "User=" + user);

        List<User> users = userService.listUsers();
        check("listUsers() size=3", users.size() == 3, "size=" + users.size());

        boolean exists = userService.exists(2);
        check("exists(2)=true", exists, "exists=" + exists);

        boolean notExists = userService.exists(99);
        check("exists(99)=false", !notExists, "exists=" + notExists);

        List<Order> orders = orderService.listByUser(1);
        check("listByUser(1) size=2", orders.size() == 2, "orders=" + orders);

        Order created = orderService.createOrder(1, "Phone", 599.0);
        check("createOrder 返回非空", created != null && "Phone".equals(created.getItem()),
                "created=" + created);

        log.info("=== Scene2 完成 ===\n");
    }

    private void check(String desc, boolean ok, String detail) {
        log.info("  [{}] {} | {}", ok ? "PASS" : "FAIL", desc, detail);
    }
}
