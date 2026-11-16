package com.demo.toefl.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    // 密钥，长度≥32字符
    private static final String SECRET = "gap-rbac-demo-secret-key-20261105abcd";
    // 过期时间：2小时
    private static final long EXPIRE_TIME = 2 * 60 * 60 * 1000L;
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    // 生成token
    public String generateToken(String username) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + EXPIRE_TIME);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expireDate)
                .signWith(KEY)
                .compact();
    }

    // 解析token，拿到用户名
    public String parseToken(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token);
        return jws.getBody().getSubject();
    }
    
}
