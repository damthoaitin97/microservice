package com.example.auth_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    public void saveRefreshToken(String username, String refreshToken, Date expiryDate) {
        String key = REFRESH_TOKEN_PREFIX + username;
        long expiryTimeInSeconds = (expiryDate.getTime() - System.currentTimeMillis()) / 1000;
        redisTemplate.opsForValue().set(key, refreshToken, expiryTimeInSeconds, TimeUnit.SECONDS);
    }

    public String getRefreshToken(String username) {
        String key = REFRESH_TOKEN_PREFIX + username;
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteRefreshToken(String username) {
        String key = REFRESH_TOKEN_PREFIX + username;
        redisTemplate.delete(key);
    }
}