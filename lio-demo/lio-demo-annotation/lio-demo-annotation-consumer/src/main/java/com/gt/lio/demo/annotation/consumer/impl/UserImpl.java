package com.gt.lio.demo.annotation.consumer.impl;

import com.gt.lio.config.annotation.LioReference;
import com.gt.lio.demo.annotation.api.database.UserDatabase;
import com.gt.lio.demo.annotation.api.inf.UserService;
import com.gt.lio.demo.annotation.api.model.User;
import com.gt.lio.demo.annotation.consumer.fallback.UserServiceFallback;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("userImpl")
public class UserImpl {

    @LioReference(fallback = UserServiceFallback.class)
    private UserService userService;

    public User selectById(Long id) {
        return userService.selectById(id);
    }

    public List<User> selectAll() {
        return userService.selectAll();
    }

    public List<User> selectByName(String keyword) {
        return userService.selectByName(keyword);
    }

    public List<User> selectByAgeRange(int minAge, int maxAge) {
        return userService.selectByAgeRange(minAge, maxAge);
    }

    public boolean insert(User user) {
        return userService.insert(user);
    }

    public boolean update(User user) {
        return userService.update(user);
    }

    public boolean deleteById(Long id) {
        return userService.deleteById(id);
    }

}
