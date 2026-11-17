package com.lingrui.usermanagement.service;

import com.lingrui.usermanagement.entity.User;
import com.lingrui.usermanagement.mapper.UserMapper;
import com.lingrui.usermanagement.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

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
        user.setPassword(password);

        //保存到数据库
            try{
                userMapper.insert(user);
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
     * @return sessionId
     */
    public String login(String username, String password){

        //检查输入
        if(username == null || username.isBlank() ||password == null || password.isBlank()){
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "用户名和密码不能为空"
            );
        }

        //根据用户名查询数据库
        User user = userMapper.findByUserName(username);

        //校验密码
        if(user == null || !password.equals(user.getPassword())){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "用户名或密码错误"
            );
        }

        //生成随机登录凭证
        String sessionId = UUID.randomUUID().toString();

        //记录
        sessions.put(sessionId, user.getId());

        return sessionId;
    }

    /**
     * 根据登录凭证查询当前用户的信息
     */
    public UserVO getCurrentUser(String sessionId){

        if(sessionId == null || sessionId.isBlank()){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "请先登录"
            );
        }

        //从登记记录中找到用户编号
        Long userId = sessions.get(sessionId);

        if(userId == null){
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "登录凭证无效，请重新登录"
            );
        }

        //根据用户编号查询数据库
        User user = userMapper.findById(userId);

        if(user == null){
            sessions.remove(sessionId);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "登录用户已不存在，请重新登录"
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
    public List<UserVO> listUsers(String sessionId){

        //复用已有方法检查登录状态
        getCurrentUser(sessionId);

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
    public void updateUsername(String sessionId, String username){

        //验证登录凭证，并取得当前用户的信息
        UserVO currentUser = getCurrentUser(sessionId);

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
     * 检查旧密码，更新为新密码，
     * 修改成功后清除该用户的登录凭证。
     */
    public void updatePassword(
            String sessionId,
            String oldPassword,
            String newPassword) {

        // 1. 验证登录凭证，取得用户编号
        UserVO currentUser = getCurrentUser(sessionId);
        Long userId = currentUser.getId();

        // 2. 检查输入
        if (oldPassword == null || oldPassword.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "旧密码和新密码不能为空"
            );
        }

        if (oldPassword.length() > 255 || newPassword.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "密码不能超过255个字符"
            );
        }

        if (oldPassword.equals(newPassword)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "新密码不能与旧密码相同"
            );
        }

        // 3. SQL 同时检查用户编号和旧密码，匹配才更新
        int rows = userMapper.updatePassword(
                userId,
                oldPassword,
                newPassword
        );

        if (rows == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "旧密码错误或用户已不存在"
            );
        }

        // 4. 修改成功后清除该用户的登录凭证
        for (String key : sessions.keySet()) {
            Long savedUserId = sessions.get(key);

            if (userId.equals(savedUserId)) {
                sessions.remove(key, savedUserId);
            }
        }
    }

    /**
     * 删除当前登录用户的账号，并清除其登录凭证
     */
    public void deleteCurrentUser(String sessionId){

        //检查登录状态，取得当前用户
        UserVO currentUser = getCurrentUser(sessionId);
        Long userId = currentUser.getId();

        int rows = userMapper.deleteById(userId);

        for(String key : sessions.keySet()){
            Long savedUserId = sessions.get(key);

            if(userId.equals(savedUserId)){
                sessions.remove(key, savedUserId);
            }
        }

        if(rows == 0){
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "用户已不存在"
            );
        }
    }
}
