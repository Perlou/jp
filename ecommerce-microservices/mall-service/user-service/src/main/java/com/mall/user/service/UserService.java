package com.mall.user.service;

import com.mall.common.exception.BusinessException;
import com.mall.user.dto.UserDTO;
import com.mall.user.entity.User;
import com.mall.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User register(UserDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw BusinessException.of("用户名已存在");
        }
        if (dto.getPhone() != null && userRepository.existsByPhone(dto.getPhone())) {
            throw BusinessException.of("手机号已被注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());

        return userRepository.save(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional
    public User update(Long id, UserDTO dto) {
        User user = findById(id);
        if (dto.getNickname() != null)
            user.setNickname(dto.getNickname());
        if (dto.getPhone() != null)
            user.setPhone(dto.getPhone());
        if (dto.getEmail() != null)
            user.setEmail(dto.getEmail());
        if (dto.getAvatar() != null)
            user.setAvatar(dto.getAvatar());
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw BusinessException.notFound("用户不存在");
        }
        userRepository.deleteById(id);
    }
}
