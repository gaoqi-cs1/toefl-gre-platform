package com.demo.toefl.dto;

import lombok.Data;

import java.util.List;

/**
 * 真题套题整卷提交参数
 * 前端在四个模块都做完后一次性提交，answers 里只包含"用户真正答过"的题。
 */
@Data
public class FullSubmitDTO {

    /** 模考记录 id（由 /api/fullExam/start 返回） */
    private Long recordId;

    /** 前端计时器给出的总用时（秒），可为空，后端会按 startTime→endTime 兜底 */
    private Integer costSeconds;

    private List<Item> answers;

    @Data
    public static class Item {
        /** 题目 id（full_question.id） */
        private Long questionId;

        /** 客观题是 A/B/C/D；口语写作是文本（口述转写或作文） */
        private String userAnswer;

        /**
         * 口语录音地址（先调 /api/file/uploadAudio 上传，拿到的 /upload/audio/xxx.webm）。
         * 只有口语题会带；后端存进 full_exam_answer.audio_url，成绩页可回放。
         */
        private String audioUrl;
    }
}
