package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * GRE 错题本（独立于托福错题本 wrong_book）
 * 分块练习答错、套题提交判错都会写进这张表；
 * 同一用户同一题只留一条，wrong_count 累加，掌握后把 mastered 置 1。
 */
@Data
@TableName("gre_wrong")
public class GreWrong {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户id */
    private Long userId;

    /** 题目id（practice=gre_question.id，suite=gre_suite_question.id） */
    private Long questionId;

    /** practice=分块练习题 / suite=套题里的题（两套题库 id 独立，靠它区分） */
    private String questionScope;

    /** 来源练习单元 */
    private Long sectionId;

    /** writing / verbal / quant */
    private String moduleType;

    /** text_completion / sentence_equivalence / reading / quant_compare / quant_problem */
    private String taskType;

    /** 题干快照 */
    private String questionTitle;

    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String optionE;
    private String optionF;

    /** 正确答案快照 */
    private String correctAnswer;

    /** 最近一次错误作答 */
    private String userAnswer;

    /** 解析快照（中英双语） */
    private String analysis;

    /** 累计错误次数 */
    private Integer wrongCount;

    /** 最近一次来源：practice / exam */
    private String lastSource;

    /** 1=已标记掌握，列表默认过滤掉 */
    private Integer mastered;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
