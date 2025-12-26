package com.example.jp_user_service.service;

import com.example.jp_user_service.dto.UserRequest;
import com.example.jp_user_service.dto.UserResponse;
import com.example.jp_user_service.entity.User;
import com.example.jp_user_service.exception.DuplicateResourceException;
import com.example.jp_user_service.exception.ResourceNotFoundException;
import com.example.jp_user_service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse create(UserRequest request) {

    }

}
