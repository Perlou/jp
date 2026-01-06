package com.example.seckill.service;

import com.example.seckill.common.SeckillException;
import com.example.seckill.entity.User;
import com.example.seckill.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 用户注册
     */
    public User register(String username, String password) {
        // 检查用户名是否存在
        if (userMapper.findByUsername(username).isPresent()) {
            throw new SeckillException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // 生产环境需要加密
        user.setNickname(username);
        user.setStatus(User.STATUS_ACTIVE);

        userMapper.insert(user);
        log.info("用户注册成功: {}", username);

        return user;
    }

    /**
     * 用户登录
     */
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username)
                .orElseThrow(() -> new SeckillException("用户不存在"));

        if (!password.equals(user.getPassword())) {
            throw new SeckillException("密码错误");
        }

        if (user.getStatus() != User.STATUS_ACTIVE) {
            throw new SeckillException("用户已被禁用");
        }

        log.info("用户登录成功: {}", username);
        return user;
    }

    public User findById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new SeckillException("用户不存在");
        }
        return user;
    }

    public List<User> findAll() {
        return userMapper.selectList(null);
    }
}
