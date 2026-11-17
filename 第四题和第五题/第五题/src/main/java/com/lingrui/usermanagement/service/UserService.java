package com.lingrui.usermanagement.service;

import com.lingrui.usermanagement.entity.User;
import com.lingrui.usermanagement.mapper.UserMapper;
import com.lingrui.usermanagement.util.JwtUtil;
import com.lingrui.usermanagement.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    //注入配置类中创建的密码工具
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户注册
     * @param username
     * @param password
     */
    public void register(String username, String password){

        //检查用户名和密码
        if(username == null || username.isBlank()
        || password == null || password.isBlank()){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "用户名和密码不能为空"
            );
        }

        // 按 UTF-8 编码后的字节数检查，中文字符可能占多个字节
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "密码不能超过72字节"
            );
        }

        //与数据库允许的长度保持一致
        if(username.length() > 50 || password.length() > 255){
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "用户名或密码过长"
        );
        }

        //查询用户是否存在
        User existingUser = userMapper.findByUserName(username);

        if(existingUser != null){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "用户名已存在"
            );
        }

        //把注册信息装进 User 对象
        User user = new User();
        user.setUsername(username);
        String passwordHash = passwordEncoder.encode(password);
        user.setPassword(passwordHash);

        //保存到数据库
            try{
                userMapper.insert(user);

                //插入成功后记录
                log.info("注册成功，用户编号={}", user.getId());
            } catch(DuplicateKeyException e){
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "用户名已存在"
                );
            }
    }

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return JWT登录凭证
     */
    public String login(String username, String password){

        //检查输入
        if(username == null || username.isBlank() ||password == null || password.isBlank()){

            log.warn("登录失败，用户名={}，原因=用户名或密码为空", username);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "用户名和密码不能为空"
            );
        }

        //根据用户名查询数据库
        User user = userMapper.findByUserName(username);

        //校验密码
        if(user == null || !passwordEncoder.matches(password, user.getPassword())){

            log.warn("登录失败，用户名={}，原因=用户名或密码错误", username);

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "用户名或密码错误"
            );
        }

        //用户名和密码验证通过以后，生成 JWT
        String token = jwtUtil.generateToken(user.getId());

        log.info("登录成功，用户编号={}", user.getId());

        return token;
    }

    /**
     * 校验 JWT，并查询当前登录用户
     */
    public UserVO getCurrentUser(String authorization){

        // 检查请求头是否携带 Bearer 凭证
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "请先登录"
            );
        }

        //去掉 "Bearer " 前缀，取得 JWT
        String token = authorization.substring(7);

        // 验证签名和过期时间，取出用户编号
        Long userId = jwtUtil.parseUserId(token);

        // 查询用户资料，同时确认账号仍然存在
        User user = userMapper.findById(userId);

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "登录用户已不存在"
            );
        }

        //把允许返回的信息装进 VO
        UserVO result = new UserVO();
        result.setUsername(user.getUsername());
        result.setId(user.getId());

        return result;
    }

    /**
     * 查询用户列表：先确认调用者已登录， 再返回用户基本信息
     */
    public List<UserVO> listUsers(String authorization){

        //复用已有方法检查登录状态
        getCurrentUser(authorization);

        List<User> users = userMapper.findAll();

        //创建一个列表，装准备返回给客户端的信息
        List<UserVO> result = new ArrayList<>();

        for(User user : users){
            UserVO vo = new UserVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());

            result.add(vo);
        }

        return result;
    }

    /**
     * 修改当前登录用户的用户名
     */
    public void updateUsername(String authorization, String username){

        //验证登录凭证，并取得当前用户的信息
        UserVO currentUser = getCurrentUser(authorization);

        //检查新用户名
        if(username == null ||username.isBlank()){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "用户名不能为空"
            );
        }

        if(username.length() > 50){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "用户名不能超过50个字符"
            );
        }

        //查找是否已经有人在用这个名字
        User existingUser = userMapper.findByUserName(username);

        if(existingUser != null && !existingUser.getId().equals(currentUser.getId())){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "用户名已存在"
            );
        }

        //修改的用户编号来自登录凭证，不能由客户端指正
        try{
            int row = userMapper.updateUsername(
                    currentUser.getId(),
                    username
            );

            //防止验证身份之后，用户记录已经被其他请求删除
            if(row == 0){
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "用户不存在"
                );
            }
        } catch (DuplicateKeyException e){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "用户名已存在"
            );
        }
    }

    /**
     * 修改密码
     */
    public void updatePassword(String authorization,
                               String oldPassword,
                               String newPassword){

        //验证凭证，取得当前用户编号
        UserVO currentUser = getCurrentUser(authorization);
        Long userId = currentUser.getId();

        if(oldPassword == null || oldPassword.isBlank() || newPassword == null || newPassword.isBlank()){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "旧密码和新密码不能为空"
            );
        }

        // BCrypt 的输入不能超过72字节
        if (oldPassword.getBytes(StandardCharsets.UTF_8).length > 72
                || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "密码不能超过72字节"
            );
        }

        User user = userMapper.findUserWithPasswordById(userId);

        if(user == null ){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "用户不存在"
            );
        }

        String oldPasswordHash = user.getPassword();

        if(!passwordEncoder.matches(oldPassword, oldPasswordHash)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "旧密码错误"
            );
        }

        //旧密码已验证正确，可以直接比较两次输入
        if(oldPassword.equals(newPassword)){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "新密码不能与旧密码相同"
            );
        }

        //生成新密码哈希
        String newPasswordHash = passwordEncoder.encode(newPassword);

        int rows = userMapper.updatePassword(
                userId,
                oldPasswordHash,
                newPasswordHash
        );

        if(rows == 0){
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "用户信息已发生变化，请重新登录后重试"
            );
        }

        // 密码更新和登录凭证清理完成
        log.info("修改密码成功，用户编号={}", userId);
    }

    /**
     * 删除当前登录用户的账号
     */
    public void deleteCurrentUser(String authorization){

        //检查登录状态，取得当前用户
        UserVO currentUser = getCurrentUser(authorization);
        Long userId = currentUser.getId();

        int rows = userMapper.deleteById(userId);

        if(rows == 0){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "用户已不存在"
            );
        }
    }
}
