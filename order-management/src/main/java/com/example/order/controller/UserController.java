package com.example.order.controller;

import com.example.order.common.Result;
import com.example.order.entity.User;
import com.example.order.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<User> register(@RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String phone) {
        return Result.success(userService.register(username, password, phone));
    }

    @PostMapping("/login")
    public Result<User> login(@RequestParam String username,
            @RequestParam String password) {
        return Result.success(userService.login(username, password));
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
    public Result<User> update(@PathVariable Long id,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status) {
        return Result.success(userService.update(id, phone, status));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }
}
