package com.mall.user.controller;

import com.mall.common.result.Result;
import com.mall.user.dto.UserDTO;
import com.mall.user.entity.User;
import com.mall.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody UserDTO dto) {
        return Result.success(userService.register(dto));
    }

    @GetMapping("/{id}")
    public Result<User> findById(@PathVariable Long id) {
        return Result.success(userService.findById(id));
    }

    @GetMapping
    public Result<List<User>> findAll() {
        return Result.success(userService.findAll());
    }

    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody UserDTO dto) {
        return Result.success(userService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }
}
