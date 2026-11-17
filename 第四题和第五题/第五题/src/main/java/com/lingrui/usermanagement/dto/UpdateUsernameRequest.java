package com.lingrui.usermanagement.dto;

/**
 * 接收修改用户名的请求
 */

import lombok.Data;

@Data
public class UpdateUsernameRequest {

    //用户想改成的新名字
    private String username;
}
