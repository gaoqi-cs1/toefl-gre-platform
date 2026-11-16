package com.demo.toefl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.toefl.entity.GreQuestion;
import com.demo.toefl.entity.GreSuiteQuestion;
import com.demo.toefl.entity.GreWrong;
import com.demo.toefl.mapper.GreWrongMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * GRE 错题本写入逻辑（分块练习和套题提交共用）
 *
 * 关键点：分块练习和套题是两套独立题库，题目 id 各自编号，所以错题表多一个
 * question_scope（practice / suite）来区分，唯一键是 (user_id, question_id, question_scope)，
 * 这样"练习第 101 题"和"套题里 id=101 的另一道题"不会互相覆盖。
 *
 * 规则：
 *   1) 答错 → 写入或累加 wrong_count，mastered 重置为 0
 *   2) 之后答对 → 直接从错题本移除
 *   3) 写作题没有唯一正确答案，不进错题本
 */
@Service
public class GreWrongService {

    @Autowired
    private GreWrongMapper wrongMapper;

    /** 分块练习答错 */
    public void recordPracticeWrong(Long userId, GreQuestion question, String userAnswer) {
        if (question == null || isWriting(question.getTaskType())) {
            return;
        }
        GreWrong snapshot = new GreWrong();
        snapshot.setSectionId(question.getSectionId());
        snapshot.setModuleType(question.getModuleType());
        snapshot.setTaskType(question.getTaskType());
        snapshot.setQuestionTitle(question.getQuestionTitle());
        snapshot.setOptionA(question.getOptionA());
        snapshot.setOptionB(question.getOptionB());
        snapshot.setOptionC(question.getOptionC());
        snapshot.setOptionD(question.getOptionD());
        snapshot.setOptionE(question.getOptionE());
        snapshot.setOptionF(question.getOptionF());
        snapshot.setCorrectAnswer(question.getAnswer());
        snapshot.setAnalysis(question.getAnalysis());
        save(userId, question.getId(), "practice", snapshot, userAnswer, "practice");
    }

    /** 套题提交时判错 */
    public void recordSuiteWrong(Long userId, GreSuiteQuestion question, String userAnswer) {
        if (question == null || isWriting(question.getTaskType())) {
            return;
        }
        GreWrong snapshot = new GreWrong();
        snapshot.setModuleType(question.getModuleType());
        snapshot.setTaskType(question.getTaskType());
        snapshot.setQuestionTitle(question.getQuestionTitle());
        snapshot.setOptionA(question.getOptionA());
        snapshot.setOptionB(question.getOptionB());
        snapshot.setOptionC(question.getOptionC());
        snapshot.setOptionD(question.getOptionD());
        snapshot.setOptionE(question.getOptionE());
        snapshot.setOptionF(question.getOptionF());
        snapshot.setCorrectAnswer(question.getAnswer());
        snapshot.setAnalysis(question.getAnalysis());
        save(userId, question.getId(), "suite", snapshot, userAnswer, "exam");
    }

    /** 分块练习答对：从错题本移除 */
    public void releasePractice(Long userId, Long questionId) {
        remove(userId, questionId, "practice");
    }

    /** 套题里答对：也从错题本移除 */
    public void releaseSuite(Long userId, Long questionId) {
        remove(userId, questionId, "suite");
    }

    // ==================================================================
    // 内部：查重 + 累加
    // ==================================================================
    private void save(Long userId, Long questionId, String scope, GreWrong snapshot,
                      String userAnswer, String source) {
        if (userId == null || questionId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        GreWrong exist = wrongMapper.selectOne(new LambdaQueryWrapper<GreWrong>()
                .eq(GreWrong::getUserId, userId)
                .eq(GreWrong::getQuestionId, questionId)
                .eq(GreWrong::getQuestionScope, scope)
                .last("limit 1"));
        if (exist != null) {
            exist.setUserAnswer(userAnswer);
            exist.setWrongCount(exist.getWrongCount() == null ? 1 : exist.getWrongCount() + 1);
            exist.setLastSource(source);
            exist.setMastered(0);
            exist.setUpdateTime(now);
            wrongMapper.updateById(exist);
            return;
        }
        snapshot.setUserId(userId);
        snapshot.setQuestionId(questionId);
        snapshot.setQuestionScope(scope);
        snapshot.setUserAnswer(userAnswer);
        snapshot.setWrongCount(1);
        snapshot.setLastSource(source);
        snapshot.setMastered(0);
        snapshot.setCreateTime(now);
        snapshot.setUpdateTime(now);
        wrongMapper.insert(snapshot);
    }

    private void remove(Long userId, Long questionId, String scope) {
        if (userId == null || questionId == null) {
            return;
        }
        wrongMapper.delete(new LambdaQueryWrapper<GreWrong>()
                .eq(GreWrong::getUserId, userId)
                .eq(GreWrong::getQuestionId, questionId)
                .eq(GreWrong::getQuestionScope, scope));
    }

    private boolean isWriting(String taskType) {
        return taskType != null && taskType.toLowerCase().startsWith("awa");
    }
}
