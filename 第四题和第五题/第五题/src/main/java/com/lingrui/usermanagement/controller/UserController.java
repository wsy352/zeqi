package com.lingrui.usermanagement.controller;

import com.lingrui.usermanagement.dto.RegisterRequest;
import com.lingrui.usermanagement.dto.UpdatePasswordRequest;
import com.lingrui.usermanagement.dto.UpdateUsernameRequest;
import com.lingrui.usermanagement.service.UserService;
import com.lingrui.usermanagement.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 查询当前登录用户：GET /users/me
     */
    @GetMapping("/me")
    public UserVO me(@RequestHeader(value = "Authorization", required = false) String authorization){

        return userService.getCurrentUser(authorization);
    }

    /**
     * 查询用户列表：GET /users
     */
    @GetMapping
    public List<UserVO> listUsers(
            @RequestHeader(value = "Authorization", required = false) String authorization){

        return userService.listUsers(authorization);
    }

    /**
     * 修改自己的用户名：PATCH /users/me
     */
    @PatchMapping("/me")
    public String updateUsername(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody UpdateUsernameRequest request
            ){

        userService.updateUsername(authorization, request.getUsername());

        return "用户名修改成功";
    }

    /**
     * 修改自己的密码：PUT /users/me/password
     */
    @PutMapping("/me/password")
    public String updatePassword(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody UpdatePasswordRequest request
            ){

        userService.updatePassword(
                authorization,
                request.getOldPassword(),
                request.getNewPassword()
        );

        return "密码修改成功";
    }

    // 删除当前登录的用户
    @DeleteMapping("/me")
    public String deleteCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        // 根据登录凭证确定用户，再删除账号
        userService.deleteCurrentUser(authorization);

        return "账号删除成功";
    }

}
