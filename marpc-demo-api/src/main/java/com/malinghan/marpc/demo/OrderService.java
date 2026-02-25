package com.malinghan.marpc.demo;

import java.util.List;

public interface OrderService {
    Order getOrder(int id);
    List<Order> listByUser(int userId);
    Order createOrder(int userId, String item, double price);
}
