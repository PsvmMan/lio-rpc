package com.gt.lio.demo.annotation.api.inf;


import com.gt.lio.config.annotation.LioNoFallback;
import com.gt.lio.config.annotation.LioReferenceMethod;
import com.gt.lio.config.annotation.LioServiceMethod;
import com.gt.lio.demo.annotation.api.callback.UserCallback;
import com.gt.lio.demo.annotation.api.model.User;
import com.gt.lio.limiter.LioRateLimit;

import java.util.List;

public interface UserService {

    /**
     * 根据用户ID查询用户信息
     * @param id 用户ID
     * @return User对象，如果不存在则返回null
     */
    @LioReferenceMethod(isAsync = false, callback = UserCallback.class, cluster = "simple", loadBalance = "weightedRandom")
//    @LioRateLimit(period = 20000, capacity = 3, refillTokens = 1)
    User selectById(Long id);

    /**
     * 查询所有用户
     * @return 用户列表
     */
    @LioReferenceMethod(timeout = 8000)
    @LioServiceMethod(isCompressed = true,compressionType = "zstd")
    List<User> selectAll();

    /**
     * 根据用户名模糊查询
     * @param keyword 名称关键字
     * @return 匹配的用户列表
     */
    @LioReferenceMethod(timeout = 5000)
    List<User> selectByName(String keyword);

    /**
     * 根据年龄范围查询用户
     * @param minAge 最小年龄（包含）
     * @param maxAge 最大年龄（包含）
     * @return 用户列表
     */
    @LioReferenceMethod(timeout = 8000)
    List<User> selectByAgeRange(int minAge, int maxAge);

    /**
     * 插入一个新用户
     * @param user 用户对象
     * @return 是否插入成功（如ID冲突则失败）
     */
    @LioReferenceMethod(timeout = 3000)
    @LioNoFallback
    boolean insert(User user);

    /**
     * 更新用户信息
     * @param user 新的用户对象（需包含ID）
     * @return 是否更新成功（如果ID不存在则失败）
     */
    @LioReferenceMethod(timeout = 3000)
    boolean update(User user);

    /**
     * 根据ID删除用户
     * @param id 用户ID
     * @return 是否删除成功（如果ID不存在也返回false）
     */
    @LioReferenceMethod(timeout = 3000)
    boolean deleteById(Long id);
}