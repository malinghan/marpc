package com.malinghan.marpc.demo;

import java.util.List;

public interface UserService {
    User getUser(int id);
    List<User> listUsers();
    boolean exists(int id);
}
