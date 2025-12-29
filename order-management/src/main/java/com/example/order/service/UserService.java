package com.example.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.order.entity.User;
import com.example.order.exception.BusinessException;
import com.example.order.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务
 */
@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User register(String username, String password, String phone) {
        // 检查用户名是否存在
        if (userMapper.findByUsername(username).isPresent()) {
            throw BusinessException.of("用户名已存在");
        }
        // 检查手机号
        if (phone != null && userMapper.findByPhone(phone).isPresent()) {
            throw BusinessException.of("手机号已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(password); // 实际应加密
        user.setPhone(phone);
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    public User login(String username, String password) {
        User user = userMapper.findByUsername(username)
                .orElseThrow(() -> BusinessException.of("用户不存在"));
        if (!user.getPassword().equals(password)) {
            throw BusinessException.of("密码错误");
        }
        if (user.getStatus() != 1) {
            throw BusinessException.of("用户已被禁用");
        }
        return user;
    }

    public User findById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    public List<User> findAll() {
        return userMapper.selectList(null);
    }

    public Page<User> findPage(int pageNum, int pageSize) {
        return userMapper.selectPage(new Page<>(pageNum, pageSize), null);
    }

    public User update(Long id, String phone, Integer status) {
        User user = findById(id);
        if (phone != null)
            user.setPhone(phone);
        if (status != null)
            user.setStatus(status);
        userMapper.updateById(user);
        return user;
    }

    public void delete(Long id) {
        userMapper.deleteById(id);
    }
}
