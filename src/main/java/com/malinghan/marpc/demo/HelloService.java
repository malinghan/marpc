package com.malinghan.marpc.demo;

import java.util.List;

public interface HelloService {
    String hello(String name);
    // 重载：同名不同参数类型
    String hello(String name, int times);
    int add(int a, int b);
    List<String> list(String prefix, int count);
    User getUser(int id);
}
