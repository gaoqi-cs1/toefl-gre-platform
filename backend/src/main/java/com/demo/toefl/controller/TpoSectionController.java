package com.demo.toefl.controller;

import com.demo.toefl.entity.TpoSection;
import com.demo.toefl.service.TpoSectionService;
import com.demo.toefl.util.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/section")
public class TpoSectionController {
    @Resource
    private TpoSectionService tpoSectionService;

    /**
     * 根据id获取篇章
     */
    @GetMapping("/{id}")
    public Result<TpoSection> getById(@PathVariable Long id){
        TpoSection section = tpoSectionService.getById(id);
        if(section == null){
            return Result.fail("篇章不存在");
        }
        return Result.success(section);
    }
}
