package com.demo.toefl.dto;
import lombok.Data;
import java.util.List;

@Data
public class ExamSubmitDTO {
    private Long recordId;
    // 阅读作答
    private List<QuestionAnswerDTO> readingAnswerList;
    // 听力作答
    private List<QuestionAnswerDTO> listeningAnswerList;
    // 口语作答
    private List<SpeakWriteDTO> speakingAnswerList;
    // 写作作答
    private List<SpeakWriteDTO> writingAnswerList;

    @Data
    public static class QuestionAnswerDTO{
        private Long questionId;
        private String userAns;
    }
    @Data
    public static class SpeakWriteDTO{
        private Long questionId;
        private String content;
    }
}
