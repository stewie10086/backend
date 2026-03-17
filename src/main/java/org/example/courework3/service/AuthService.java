package org.example.courework3.service;

import lombok.RequiredArgsConstructor;
import org.example.courework3.entity.User;
import org.example.courework3.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public void register(String email, String code, String rawPassword) {
        // 校验验证码
        String cachedCode = redisTemplate.opsForValue().get("captcha:" + email);
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 检查用户是否已存在
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("该邮箱已被注册");
        }

        // 创建新用户
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword)); // 加密密码

        // 你的 SQL 要求 university_id 和 username 必填，这里先用 email 代替处理
        user.setUniversityId("UID_" + System.currentTimeMillis());
        user.setUsername(email.split("@")[0]);

        userRepository.save(user);

        // 注册成功后删除验证码
        redisTemplate.delete("captcha:" + email);
    }
}