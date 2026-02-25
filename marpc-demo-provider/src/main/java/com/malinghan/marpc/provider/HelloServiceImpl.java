package com.malinghan.marpc.provider;

import com.malinghan.marpc.annotation.MarpcProvider;
import com.malinghan.marpc.demo.HelloService;
import com.malinghan.marpc.demo.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@MarpcProvider
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        return "hello, " + name;
    }

    @Override
    public String hello(String name, int times) {
        return (name + " ").repeat(times).trim();
    }

    @Override
    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public List<String> list(String prefix, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> prefix + i)
                .collect(Collectors.toList());
    }

    @Override
    public User getUser(int id) {
        return new User(id, "user-" + id);
    }
}
