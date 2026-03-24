package org.example.courework3.controller;


import lombok.extern.slf4j.Slf4j;
import org.example.courework3.result.Result;
import org.example.courework3.result.UserResult;
import org.example.courework3.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@Slf4j
public class UserInfoController {

    @Autowired
    private AuthService authService;

    @GetMapping("/me")
    public Result<UserResult> getSelfInfo(@RequestHeader("Authorization") String authHeader){
        String token = authHeader.replace("Bearer ", "");
        String userId = authService.getUserIdByToken(token);
        UserResult result = new UserResult();
        result.toDTO(authService.getSelfInfo(userId));
        return Result.success(result.toDTO(authService.getSelfInfo(userId)));
    }

}
