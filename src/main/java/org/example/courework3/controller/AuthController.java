package org.example.courework3.controller;

import jakarta.validation.Valid;
import org.example.courework3.dto.CaptchaRequest;
import org.example.courework3.dto.LoginRequest;
import org.example.courework3.dto.RegisterRequest;
import org.example.courework3.service.AliyunMailService;
import org.example.courework3.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private AliyunMailService mailService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 固定账号密码校验
        if ("123@qq.com".equals(request.getEmail()) && "123".equals(request.getPassword())) {
            Map<String, Object> map = new HashMap<>();
            map.put("code", 200);
            map.put("message", "登录成功");
            map.put("token", "UUID-TEST-TOKEN-" + UUID.randomUUID());

            // 模拟用户信息
            Map<String, Object> user = new HashMap<>();
            user.put("email", request.getEmail());
            user.put("name", "测试用户");
            user.put("role", "Customer");
            map.put("user", user);
            System.out.println("1");
            return ResponseEntity.ok(map);

        } else {
            System.out.println("2");
            return ResponseEntity.status(401).body(Map.of("message", "用户名或密码错误"));
        }
    }
    @PostMapping("/send-email-code")
    public ResponseEntity<?> sendCaptcha(@Valid @RequestBody CaptchaRequest request) {
        try {
            mailService.sendCaptcha(request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "code", "OK",
                    "message", "验证码已发送"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "code", "ERROR",
                    "message", "邮件发送失败：" + e.getMessage()
            ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.register(request.getEmail(), request.getVerificationCode(), request.getPassword());
        return ResponseEntity.ok("注册成功");
    }
}