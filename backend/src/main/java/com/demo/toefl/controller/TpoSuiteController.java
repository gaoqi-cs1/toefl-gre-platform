package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.entity.TpoSuite;
import com.demo.toefl.service.TpoSuiteService;
import com.demo.toefl.util.Result;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/tposuite")
public class TpoSuiteController {

    @Resource
    private TpoSuiteService tpoSuiteService;

    @GetMapping("/list")
    public Result<List<TpoSuite>> listSuite() {
        LambdaQueryWrapper<TpoSuite> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(TpoSuite::getId);
        List<TpoSuite> list = tpoSuiteService.list(wrapper);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<TpoSuite> getById(@PathVariable Long id) {
        TpoSuite suite = tpoSuiteService.getById(id);
        return Result.success(suite);
    }

    @GetMapping("/get")
    public Result<?> getSuiteById(@RequestParam Long suiteId){
        TpoSuite suite = tpoSuiteService.getById(suiteId);
        if(suite == null){
            return Result.fail("套题不存在");
        }
        return Result.success(suite);
    }
}
