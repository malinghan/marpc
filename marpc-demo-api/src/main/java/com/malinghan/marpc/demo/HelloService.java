package com.malinghan.marpc.demo;

import java.util.List;

public interface HelloService {
    String hello(String name);
    String hello(String name, int times);
    int add(int a, int b);
    List<String> list(String prefix, int count);
    User getUser(int id);
}
