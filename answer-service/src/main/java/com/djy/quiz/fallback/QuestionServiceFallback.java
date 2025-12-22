// answer-service/src/main/java/com/djy/quiz/feign/fallback/QuestionServiceFallback.java
package com.djy.quiz.fallback;

import com.djy.quiz.feign.QuestionServiceClient;
import com.djy.quiz.pojo.dto.QuestionDTO;
import com.djy.quiz.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component  // 必须标记为Spring组件
public class QuestionServiceFallback implements QuestionServiceClient {

    @Override
    public Result<QuestionDTO> getQuestionById(Integer id) {
        log.warn("QuestionService降级: 获取题目[id={}]失败，返回默认题目", id);

        // 返回一个默认题目（降级逻辑）
        QuestionDTO defaultQuestion = new QuestionDTO();
        defaultQuestion.setQuestionId(id);
        defaultQuestion.setQuestionText("【服务降级】默认题目");
        defaultQuestion.setAnswer1Text("选项A");
        defaultQuestion.setAnswer2Text("选项B");
        defaultQuestion.setAnswer3Text("选项C");
        defaultQuestion.setAnswer4Text("选项D");
        defaultQuestion.setAnswer1Correct(1); // 默认A正确

        return Result.ok(defaultQuestion);
    }

    @Override
    public Result<List<QuestionDTO>> getAllQuestions() {
        log.warn("QuestionService降级: 获取所有题目失败，返回空列表");
        return Result.ok(new ArrayList<>()); // 返回空列表
    }

    // 或者返回错误结果
    public Result<List<QuestionDTO>> getAllQuestionsWithResult() {
        log.error("QuestionService熔断: 服务不可用");
        return Result.error(503, "题目服务暂时不可用，请稍后重试");
    }
}