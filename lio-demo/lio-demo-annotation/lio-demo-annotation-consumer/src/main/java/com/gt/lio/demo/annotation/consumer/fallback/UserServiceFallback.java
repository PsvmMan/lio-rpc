package com.gt.lio.demo.annotation.consumer.fallback;

import com.gt.lio.demo.annotation.api.inf.UserService;
import com.gt.lio.demo.annotation.api.model.User;

import java.util.List;

public class UserServiceFallback implements UserService {

    @Override
    public User selectById(Long id) {
        return null;
    }

    @Override
    public List<User> selectAll() {
        return null;
    }

    @Override
    public List<User> selectByName(String keyword) {
        return null;
    }

    @Override
    public List<User> selectByAgeRange(int minAge, int maxAge) {
        return null;
    }

    @Override
    public boolean insert(User user) {
        return false;
    }

    @Override
    public boolean update(User user) {
        return false;
    }

    @Override
    public boolean deleteById(Long id) {
        return false;
    }
}
