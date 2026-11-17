package com.lingrui.usermanagement.mapper;

import com.lingrui.usermanagement.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    //根据用户名查询用户
    @Select("""
            SELECT id, username, password
            FROM users
            WHERE username = #{username}
            """)
    User findByUserName(@Param("username") String username);

    //新增用户
    @Insert("""
    INSERT INTO users (username, password)
    VALUES (#{username}, #{password})
""")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    //根据 id 查询用户
    @Select("""
SELECT id, username
FROM users
WHERE id = #{id}
""")
    User findById(@Param("id") Long id);

    //查询所有用户的基本信息，不要密码
    @Select("""
SELECT id, username
FROM users
ORDER BY id
""")
    List<User> findAll();

    //根据用户编号修改用户名
    @Update("""
UPDATE users
SET username = #{username}
WHERE id = #{id}
""")
    int updateUsername(@Param("id") Long id,
                       @Param("username") String username);

    //修改密码
    @Update("""
UPDATE users
SET  password = #{newPassword}
WHERE id = #{id}
AND BINARY password = BINARY #{oldPassword}
""")
    int updatePassword(@Param("id") Long id,
                       @Param("oldPassword") String oldPassword,
                       @Param("newPassword") String newPassword);

    //根据用户编号删除账号
    @Delete("""
DELETE FROM users
WHERE id =#{id}
""")
    int deleteById(@Param("id") Long id);

}


