package com.malinghan.marpc.provider;

import com.malinghan.marpc.annotation.MarpcProvider;
import com.malinghan.marpc.demo.Order;
import com.malinghan.marpc.demo.OrderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@MarpcProvider
public class OrderServiceImpl implements OrderService {

    private final Map<Integer, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger idGen = new AtomicInteger(1);

    public OrderServiceImpl() {
        orders.put(1, new Order(1, "Book", 29.9, 1));
        orders.put(2, new Order(2, "Pen", 5.5, 1));
        orders.put(3, new Order(3, "Laptop", 999.0, 2));
    }

    @Override
    public Order getOrder(int id) {
        return orders.get(id);
    }

    @Override
    public List<Order> listByUser(int userId) {
        return orders.values().stream()
                .filter(o -> o.getUserId() == userId)
                .collect(Collectors.toList());
    }

    @Override
    public Order createOrder(int userId, String item, double price) {
        int id = idGen.getAndIncrement();
        Order order = new Order(id, item, price, userId);
        orders.put(id, order);
        return order;
    }
}
