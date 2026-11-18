package com.campus.activity.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 签发与校验。
 * 只放 userId / username / role 三个轻量字段，不放敏感信息。
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expire-hours:2}")
    private long expireHours;

    private Algorithm algorithm() {
        return Algorithm.HMAC256(secret);
    }

    public String createToken(Integer userId, String username, String role) {
        long expireAt = System.currentTimeMillis() + expireHours * 3600L * 1000L;
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withClaim("role", role)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(expireAt))
                .sign(algorithm());
    }

    /**
     * 校验并解析 token，失败会抛 JWTVerificationException。
     */
    public DecodedJWT verify(String token) {
        JWTVerifier verifier = JWT.require(algorithm()).build();
        return verifier.verify(token);
    }
}
