package org.example.courework3.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.courework3.dto.ExpertiseDto;
import org.example.courework3.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
//@RequestMapping("/auth")
@CrossOrigin
@Slf4j
public class ViewController {

    @GetMapping("/expertise")
    public Result<List<ExpertiseDto>> getExpertiseList() {
        List<ExpertiseDto> list = Arrays.asList(
                new ExpertiseDto("exp-1", "职业发展", "职业规划与咨询"),
                new ExpertiseDto("exp-2", "情绪压力", "情绪管理与减压"),
                new ExpertiseDto("exp-3", "学业辅导", "学习方法与目标管理"),
                new ExpertiseDto("exp-4", "人际关系", "沟通技巧与关系处理")
        );

        return Result.success(list);
    }
}