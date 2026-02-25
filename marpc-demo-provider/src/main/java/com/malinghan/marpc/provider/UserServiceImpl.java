package com.malinghan.marpc.provider;

import com.malinghan.marpc.annotation.MarpcProvider;
import com.malinghan.marpc.demo.User;
import com.malinghan.marpc.demo.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@MarpcProvider
public class UserServiceImpl implements UserService {

    private final Map<Integer, User> users = new ConcurrentHashMap<>();

    public UserServiceImpl() {
        users.put(1, new User(1, "Alice"));
        users.put(2, new User(2, "Bob"));
        users.put(3, new User(3, "Charlie"));
    }

    @Override
    public User getUser(int id) {
        return users.get(id);
    }

    @Override
    public List<User> listUsers() {
        return List.copyOf(users.values());
    }

    @Override
    public boolean exists(int id) {
        return users.containsKey(id);
    }
}
