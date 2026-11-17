package com.lingrui.usermanagement.dto;

import lombok.Data;

/**
 * 接收客户端提交的登录信息
 */
@Data
public class LoginRequest {

    //登录用户名
    private String username;

    //登录密码
    private String password;
}
