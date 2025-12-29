package com.example.demo.service;

import com.example.demo.dto.UserRequest;
import com.example.demo.dto.UserResponse;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 创建用户
     */
    UserResponse create(UserRequest request);

    /**
     * 根据ID查找用户
     */
    UserResponse findById(Long id);

    /**
     * 查找所有用户
     */
    List<UserResponse> findAll();

    /**
     * 更新用户
     */
    UserResponse update(Long id, UserRequest request);

    /**
     * 删除用户
     */
    void delete(Long id);
}
