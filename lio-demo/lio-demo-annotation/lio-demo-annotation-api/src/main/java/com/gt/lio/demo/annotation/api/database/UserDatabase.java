package com.gt.lio.demo.annotation.api.database;

import com.gt.lio.demo.annotation.api.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserDatabase {

    // 使用 Map 模拟数据库表，主键为 id，便于快速查找和操作
    private static Map<Long, User> userMap = new HashMap<>();

    static {
        // 初始化一些测试数据
        userMap.put(1L, new User(1L, "Alice", 25));
        userMap.put(2L, new User(2L, "Bob", 30));
        userMap.put(3L, new User(3L, "Charlie", 28));
        userMap.put(4L, new User(4L, "David", 35));
    }

    /**
     * 查询所有用户
     */
    public static List<User> selectAll() {
        return new ArrayList<>(userMap.values());
    }

    /**
     * 根据 ID 查询用户
     */
    public static User selectById(Long id) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(id == 3 ){
            throw new RuntimeException("模拟异常");
        }
        return userMap.get(id);
    }

    /**
     * 根据名称模糊查询
     */
    public static List<User> selectByName(String keyword) {
        return userMap.values().stream()
                .filter(user -> user.getName().contains(keyword))
                .collect(Collectors.toList());
    }

    /**
     * 根据年龄范围查询用户
     */
    public static List<User> selectByAgeRange(int minAge, int maxAge) {
        return userMap.values().stream()
                .filter(user -> user.getAge() >= minAge && user.getAge() <= maxAge)
                .collect(Collectors.toList());
    }

    /**
     * 插入一个新用户
     * 如果 ID 已存在则抛出异常或返回 false
     */
    public static boolean insert(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Invalid user");
        }
        if (userMap.containsKey(user.getId())) {
            throw new IllegalArgumentException("ID already exists");
        }
        userMap.put(user.getId(), user);
        return true;
    }

    /**
     * 更新一个已存在的用户（根据 ID）
     * 如果 ID 不存在，返回 false
     */
    public static boolean update(User user) {
        if (user == null || user.getId() == null || !userMap.containsKey(user.getId())) {
            return false;
        }
        userMap.put(user.getId(), user);
        return true;
    }

    /**
     * 删除指定 ID 的用户
     */
    public static boolean deleteById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Invalid ID");
        }
        return userMap.remove(id) != null;
    }
}
