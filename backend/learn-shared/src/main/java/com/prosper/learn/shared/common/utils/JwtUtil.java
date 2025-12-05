package com.prosper.learn.shared.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ConfigurationProperties(prefix = "custom.jwt")
public class JwtUtil {

    // 秘钥
    @Setter
    private String secret;
    // 有效时间
    @Setter
    private Long expire;
    // 用户凭证
    @Setter
    @Getter
    private String header;
    // 签发者
    @Setter
    private String issuer;

    /**
     * 生成token签名
     * @param subject
     * @return
     */
    public String createToken(String subject) {
        Date now = new Date();
        // 过期时间
        Date expireDate = new Date(now.getTime() + expire * 1000);

        //创建Signature SecretKey
        final SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        //header参数
        final Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("alg", "HS256");
        headerMap.put("typ", "JWT");

        //生成token
        String token = Jwts.builder()
                .header().empty().add(headerMap).and()
                .subject(subject)
                .issuedAt(now)
                .expiration(expireDate)
                .issuer(issuer)
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        //log.info("JWT[" + token + "]");
        return token;
    }

    /**
     * 解析token
     *
     * @param token token
     * @return
     */
    public Claims parseToken(String token) {

        Claims claims = null;
        try {
            //创建Signature SecretKey
            final SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            //log.info("Parse JWT token success");
        } catch (JwtException  e) {
            //log.info("Parse JWT error " + e.getMessage());
            return null;
        }
        return claims;
    }

    /**
     * 判断token是否过期
     *
     * @param expiration
     * @return
     */
    public boolean isExpired(Date expiration) {
        return expiration.before(new Date());
    }
}