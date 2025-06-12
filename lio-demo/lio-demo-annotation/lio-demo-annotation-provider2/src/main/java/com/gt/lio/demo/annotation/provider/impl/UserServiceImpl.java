
package com.gt.lio.demo.annotation.provider.impl;

import com.gt.lio.config.annotation.LioService;
import com.gt.lio.demo.annotation.api.database.UserDatabase;
import com.gt.lio.demo.annotation.api.inf.UserService;
import com.gt.lio.demo.annotation.api.model.User;

import java.util.List;


@LioService
public class UserServiceImpl implements UserService {

    @Override
    public User selectById(Long id) {
        return null;
    }

    @Override
    public List<User> selectAll() {
        return UserDatabase.selectAll();
    }

    @Override
    public List<User> selectByName(String keyword) {
        return UserDatabase.selectByName(keyword);
    }

    @Override
    public List<User> selectByAgeRange(int minAge, int maxAge) {
        return UserDatabase.selectByAgeRange(minAge, maxAge);
    }

    @Override
    public boolean insert(User user) {
        return UserDatabase.insert(user);
    }

    @Override
    public boolean update(User user) {
        return UserDatabase.update(user);
    }

    @Override
    public boolean deleteById(Long id) {
        return UserDatabase.deleteById(id);
    }
}
