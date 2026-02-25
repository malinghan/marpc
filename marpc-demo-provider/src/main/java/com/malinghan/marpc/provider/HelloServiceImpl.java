package com.malinghan.marpc.provider;

import com.malinghan.marpc.annotation.MarpcProvider;
import com.malinghan.marpc.context.RpcContext;
import com.malinghan.marpc.demo.HelloService;
import com.malinghan.marpc.demo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@MarpcProvider
public class HelloServiceImpl implements HelloService {

    @Override
    public String hello(String name) {
        logContext("hello");
        return "hello, " + name;
    }

    @Override
    public String hello(String name, int times) {
        logContext("hello(overload)");
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

    private void logContext(String method) {
        String grayId = RpcContext.getGrayId();
        String traceId = RpcContext.get("traceId");
        String userId = RpcContext.get("userId");
        if (grayId != null || traceId != null || userId != null) {
            log.info("[RpcContext] method={}, grayId={}, traceId={}, userId={}",
                    method, grayId, traceId, userId);
        }
    }
}

