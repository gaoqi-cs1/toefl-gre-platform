package com.demo.toefl.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("tpo_question")
public class TpoQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sectionId;
    private Long passageId;
    private String passageTitle;
    private String passageContent;
    private String audioUrl;
    private String prompt;
    private String questionTitle;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String answer;
    private String moduleType;
    private Integer limitSecond;
    private String scoreStandard;
    private String analysis;

    /** 口语 / 写作的满分范例答案（新增列 sample_answer），生成套题时会一并复制过去 */
    private String sampleAnswer;

}
