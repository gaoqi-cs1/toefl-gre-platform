package com.demo.toefl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.entity.SuiteSection;
import com.demo.toefl.entity.TpoQuestion;
import com.demo.toefl.entity.TpoSection;
import com.demo.toefl.entity.TpoSuite;
import com.demo.toefl.service.SuiteSectionService;
import com.demo.toefl.service.TpoQuestionService;
import com.demo.toefl.service.TpoSectionService;
import com.demo.toefl.service.TpoSuiteService;
import com.demo.toefl.util.Result;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/suite")
public class SuiteController {

    @Resource
    private TpoSuiteService tpoSuiteService;
    @Resource
    private SuiteSectionService suiteSectionService;
    @Resource
    private TpoSectionService tpoSectionService;
    @Resource
    private TpoQuestionService tpoQuestionService;

    /**
     * 套题列表（含各模块篇章数量）
     */
    @GetMapping("/list")
    public Result<List<SuiteVO>> list() {
        List<TpoSuite> suiteList = tpoSuiteService.list(
                new LambdaQueryWrapper<TpoSuite>().orderByDesc(TpoSuite::getId));
        List<SuiteSection> allRel = suiteSectionService.list();
        Map<Long, List<SuiteSection>> groupMap = allRel.stream()
                .collect(Collectors.groupingBy(SuiteSection::getSuiteId));

        List<SuiteVO> voList = suiteList.stream().map(suite -> {
            SuiteVO vo = new SuiteVO();
            vo.setSuiteId(suite.getId());
            vo.setTitle(suite.getTitle());
            vo.setDescription(suite.getDescription());
            vo.setReadingMinutes(suite.getReadingMinutes());
            vo.setListeningMinutes(suite.getListeningMinutes());
            vo.setSpeakingMinutes(suite.getSpeakingMinutes());
            vo.setWritingMinutes(suite.getWritingMinutes());

            List<SuiteSection> relList = groupMap.getOrDefault(suite.getId(), List.of());
            vo.setReadingCount((int) relList.stream().filter(r -> "reading".equals(r.getType())).count());
            vo.setListeningCount((int) relList.stream().filter(r -> "listening".equals(r.getType())).count());
            vo.setSpeakingCount((int) relList.stream().filter(r -> "speaking".equals(r.getType())).count());
            vo.setWritingCount((int) relList.stream().filter(r -> "writing".equals(r.getType())).count());
            vo.setTotalSections(relList.size());
            vo.setQuestionCount(countQuestions(relList));
            return vo;
        }).collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 套题详情：返回套题信息 + 关联篇章列表（含篇章类型、音频、题目数）
     * SuiteExam 页面靠这一份数据渲染四大模块
     */
    @GetMapping("/{id}")
    public Result<SuiteDetailVO> detail(@PathVariable Long id) {
        TpoSuite suite = tpoSuiteService.getById(id);
        if (suite == null) {
            return Result.fail("套题不存在");
        }
        List<SuiteSection> sectionList = suiteSectionService.list(
                new LambdaQueryWrapper<SuiteSection>()
                        .eq(SuiteSection::getSuiteId, id)
                        .orderByAsc(SuiteSection::getId));

        SuiteDetailVO vo = new SuiteDetailVO();
        vo.setSuiteId(suite.getId());
        vo.setTitle(suite.getTitle());
        vo.setDescription(suite.getDescription());
        vo.setReadingMinutes(suite.getReadingMinutes());
        vo.setListeningMinutes(suite.getListeningMinutes());
        vo.setSpeakingMinutes(suite.getSpeakingMinutes());
        vo.setWritingMinutes(suite.getWritingMinutes());

        List<SuiteSectionVO> sectionVOs = new ArrayList<>();
        for (SuiteSection rel : sectionList) {
            SuiteSectionVO sv = new SuiteSectionVO();
            sv.setSectionId(rel.getSectionId());
            sv.setType(rel.getType());
            TpoSection section = tpoSectionService.getById(rel.getSectionId());
            if (section == null) {
                // 数据库 suite_section 指向了不存在的篇章，属于脏数据，返回空篇章并标记
                sv.setType(rel.getType());
                sv.setExist(false);
                sv.setQuestionCount(0L);
            } else {
                sv.setExist(true);
                sv.setType(section.getType());
                sv.setAudioUrl(section.getAudioUrl());
                sv.setQuestionCount(tpoQuestionService.count(
                        new LambdaQueryWrapper<TpoQuestion>().eq(TpoQuestion::getSectionId, section.getId())));
            }
            sectionVOs.add(sv);
        }
        vo.setSectionList(sectionVOs);
        vo.setQuestionCount(countQuestions(sectionList));
        return Result.success(vo);
    }

    /**
     * 一键生成套题：每种类型各取一条"有题目"的篇章，写入主表 + 关联表
     * 修正点：原实现取的是 type='reading' 的第一条，可能取到没有题目的空篇章
     */
    @Transactional
    @PostMapping("/init")
    public Result<String> init() {
        TpoSection readSec = pickSectionWithQuestion("reading");
        TpoSection listenSec = pickSectionWithQuestion("listening");
        TpoSection speakSec = pickSectionWithQuestion("speaking");
        TpoSection writeSec = pickSectionWithQuestion("writing");

        List<String> missing = new ArrayList<>();
        if (readSec == null) missing.add("阅读");
        if (listenSec == null) missing.add("听力");
        if (speakSec == null) missing.add("口语");
        if (writeSec == null) missing.add("写作");
        if (!missing.isEmpty()) {
            return Result.fail("以下模块没有可用题目，请先录入 tpo_question：" + String.join("、", missing));
        }

        TpoSuite suite = new TpoSuite();
        suite.setTitle("TPO 套题 " + System.currentTimeMillis() % 10000);
        suite.setDescription("自动生成托福模考套题");
        suite.setReadingMinutes(60);
        suite.setListeningMinutes(50);
        suite.setSpeakingMinutes(17);
        suite.setWritingMinutes(30);
        suite.setCreateTime(LocalDateTime.now());
        tpoSuiteService.save(suite);

        Long suiteId = suite.getId();
        List<SuiteSection> relList = new ArrayList<>();
        relList.add(buildRel(suiteId, readSec.getId(), "reading"));
        relList.add(buildRel(suiteId, listenSec.getId(), "listening"));
        relList.add(buildRel(suiteId, speakSec.getId(), "speaking"));
        relList.add(buildRel(suiteId, writeSec.getId(), "writing"));
        suiteSectionService.saveBatch(relList);

        return Result.success("生成成功，suiteId:" + suiteId);
    }

    /** 按类型挑一条真的有题目的篇章 */
    private TpoSection pickSectionWithQuestion(String type) {
        List<TpoSection> sections = tpoSectionService.list(
                new LambdaQueryWrapper<TpoSection>()
                        .eq(TpoSection::getType, type)
                        .orderByAsc(TpoSection::getId));
        for (TpoSection section : sections) {
            long count = tpoQuestionService.count(
                    new LambdaQueryWrapper<TpoQuestion>().eq(TpoQuestion::getSectionId, section.getId()));
            if (count > 0) {
                return section;
            }
        }
        return null;
    }

    private long countQuestions(List<SuiteSection> relList) {
        long total = 0L;
        for (SuiteSection rel : relList) {
            total += tpoQuestionService.count(
                    new LambdaQueryWrapper<TpoQuestion>().eq(TpoQuestion::getSectionId, rel.getSectionId()));
        }
        return total;
    }

    private SuiteSection buildRel(Long suiteId, Long sectionId, String type) {
        SuiteSection rel = new SuiteSection();
        rel.setSuiteId(suiteId);
        rel.setSectionId(sectionId);
        rel.setType(type);
        return rel;
    }

    @Data
    public static class SuiteVO {
        private Long suiteId;
        private String title;
        private String description;
        private Integer readingMinutes;
        private Integer listeningMinutes;
        private Integer speakingMinutes;
        private Integer writingMinutes;
        private Integer readingCount;
        private Integer listeningCount;
        private Integer speakingCount;
        private Integer writingCount;
        private Integer totalSections;
        private Long questionCount;
    }

    @Data
    public static class SuiteDetailVO {
        private Long suiteId;
        private String title;
        private String description;
        private Integer readingMinutes;
        private Integer listeningMinutes;
        private Integer speakingMinutes;
        private Integer writingMinutes;
        private List<SuiteSectionVO> sectionList;
        private Long questionCount;
    }

    @Data
    public static class SuiteSectionVO {
        private Long sectionId;
        /** reading / listening / speaking / writing */
        private String type;
        private String audioUrl;
        private Long questionCount;
        /** 该篇章在 tpo_section 里是否真实存在 */
        private Boolean exist;
    }
}
