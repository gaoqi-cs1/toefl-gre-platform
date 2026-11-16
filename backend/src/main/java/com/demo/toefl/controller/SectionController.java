package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.entity.TpoQuestion;
import com.demo.toefl.entity.TpoSection;
import com.demo.toefl.service.TpoQuestionService;
import com.demo.toefl.service.TpoSectionService;
import com.demo.toefl.util.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SectionController {
    @Resource
    private TpoSectionService tpoSectionService;

    @Resource
    private TpoQuestionService tpoQuestionService;

    @GetMapping("/section")
    public Result<TpoSection> getSection(@RequestParam Long id) {
        TpoSection section = tpoSectionService.getById(id);
        if (section == null) {
            return Result.fail("篇章不存在");
        }
        return Result.success(section);
    }

    /**
     * 篇章列表 GET /api/section/list[?type=reading]
     *
     * TPO 首页之前把阅读/听力/口语/写作的 sectionId 写死成 1/2/3/4，
     * 但库里真实的篇章是 1(reading)、4(speaking)、5(writing)、6(reading)、7(listening)，
     * 于是点"听力""口语""写作"进的都是不存在的篇章，页面必空白。
     * 这个接口让首页按 type 取真实存在的、且有题目的篇章。
     */
    @GetMapping("/section/list")
    public Result<List<SectionVO>> list(@RequestParam(required = false) String type) {
        LambdaQueryWrapper<TpoSection> wrapper = new LambdaQueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            wrapper.eq(TpoSection::getType, type);
        }
        wrapper.orderByAsc(TpoSection::getId);

        List<SectionVO> voList = new ArrayList<>();
        for (TpoSection section : tpoSectionService.list(wrapper)) {
            SectionVO vo = new SectionVO();
            vo.setId(section.getId());
            vo.setTpoId(section.getTpoId());
            vo.setType(section.getType());
            vo.setAudioUrl(section.getAudioUrl());
            vo.setQuestionCount(tpoQuestionService.count(
                    new LambdaQueryWrapper<TpoQuestion>().eq(TpoQuestion::getSectionId, section.getId())));
            // 优先用库里的 title 列；老数据没有 title 才回退成正文首句
            String t = section.getTitle();
            vo.setTitle((t != null && !t.trim().isEmpty()) ? t.trim() : shortTitle(section));
            vo.setHasTranslation(section.getContentTranslation() != null
                    && !section.getContentTranslation().trim().isEmpty());
            voList.add(vo);
        }
        return Result.success(voList);
    }

    /** 取正文第一句作为列表展示标题 */
    private String shortTitle(TpoSection section) {
        String text = section.getContentText();
        if (text == null || text.trim().isEmpty()) {
            return "篇章 " + section.getId();
        }
        String first = text.trim().split("\n")[0].trim();
        return first.length() > 40 ? first.substring(0, 40) + "..." : first;
    }

    public static class SectionVO {
        private Long id;
        private Long tpoId;
        private String type;
        private String title;
        private String audioUrl;
        private Long questionCount;
        /** 该篇是否已有中文译文（前端据此决定按钮是否可点） */
        private Boolean hasTranslation;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getTpoId() { return tpoId; }
        public void setTpoId(Long tpoId) { this.tpoId = tpoId; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getAudioUrl() { return audioUrl; }
        public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

        public Long getQuestionCount() { return questionCount; }
        public void setQuestionCount(Long questionCount) { this.questionCount = questionCount; }

        public Boolean getHasTranslation() { return hasTranslation; }
        public void setHasTranslation(Boolean hasTranslation) { this.hasTranslation = hasTranslation; }
    }
}
