package com.lingrui.usermanagement.controller;

import com.lingrui.usermanagement.dto.LoginRequest;
import com.lingrui.usermanagement.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.lingrui.usermanagement.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 处理 POST /auth/register 请求
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public String register(@RequestBody RegisterRequest request){

        //取出客户端传来的用户名、密码，交给业务层处理
        userService.register(
                request.getUsername(),
                request.getPassword()
        );

        return "注册成功";
    }

    /**
     * 登录
     * 登录成功后，返回包含 sessionId 的 JSON
     */
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest request){

        //调用业务层
        String sessionId = userService.login(
                request.getUsername(),
                request.getPassword()
        );

        //把凭证装入 Map,作为响应内容
        Map<String, String> result = new HashMap<>();
        result.put("sessionId", sessionId);

        return result;
    }
}
