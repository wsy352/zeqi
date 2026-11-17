package com.lingrui.usermanagement.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key = Jwts.SIG.HS256.key().build();

    private static final long EXPIRE_TIME = 2 * 60 * 60 * 1000L;

    /**
     * 登录成功后，根据用户编号生成 JWT
     */
    public String generateToken(Long userId){
        Date now = new Date();

        return Jwts.builder()
                .subject(userId.toString()) //保存用户编号
                .issuedAt(now)              //签发时间
                .expiration(new Date(now.getTime() + EXPIRE_TIME))
                .signWith(key)
                .compact();
    }

    /**
     *校验 JWT，成功后返回用户编号
     */
    public Long parseUserId(String token) {

        try{
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e){

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "登录或凭证无效或已过期，请重新登录"
            );
        }
    }
}
