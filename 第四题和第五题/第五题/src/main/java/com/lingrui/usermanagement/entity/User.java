package com.lingrui.usermanagement.entity;

import lombok.Data;

@Data
public class User {

    //用户编号，对应数据库中的 BIGINT
    private Long id;

    //用户名
    private String username;

    //密码
    private String password;
}
