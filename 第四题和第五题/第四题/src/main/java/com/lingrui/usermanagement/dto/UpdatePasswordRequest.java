package com.lingrui.usermanagement.dto;

import lombok.Data;

/**
 * 接收修改密码的请求
 */
@Data
public class UpdatePasswordRequest {

    //旧密码
    private String oldPassword;

    //新密码
    private String newPassword;
}
