package com.lingrui.usermanagement.dto;

import lombok.Data;

/**
 * 装客户端发来的注册数据
 */
@Data
public class RegisterRequest {

    private String username;

    private String password;
}
