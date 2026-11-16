package com.demo.toefl.controller;

import com.demo.toefl.entity.TpoSection;
import com.demo.toefl.service.TpoSectionService;
import com.demo.toefl.util.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import javax.annotation.Resource;

/**
 * TPO篇章控制器（备用，路径单独区分）
 */
@RestController
@RequestMapping("/api/tpo-section") // 改成全新前缀，不和 /api/section 冲突
public class TpoController {

    @Resource
    private TpoSectionService tpoSectionService;

    /**
     * 根据id获取篇章
     * 访问地址：GET /api/tpo-section?id=1
     */
    @GetMapping
    public Result<TpoSection> getSectionById(@RequestParam("id") Long id) {
        System.out.println("【查询篇章】id = " + id);
        TpoSection section = tpoSectionService.getById(id);
        System.out.println("【篇章结果】" + section);
        return Result.success(section);
    }
    @GetMapping("/suite/list")
public Result<List<TpoSection>> suiteList(){
    List<TpoSection> list = tpoSectionService.list();
    return Result.success(list);
}

}
