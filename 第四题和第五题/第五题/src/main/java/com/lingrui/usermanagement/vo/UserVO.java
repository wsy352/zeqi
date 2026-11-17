package com.lingrui.usermanagement.vo;

import lombok.Data;

/**
 * 返回给客户端的用户信息
 * 只包含编号和用户名，不包含密码
 */
@Data
public class UserVO {

    private Long id;

    private String username;
}
