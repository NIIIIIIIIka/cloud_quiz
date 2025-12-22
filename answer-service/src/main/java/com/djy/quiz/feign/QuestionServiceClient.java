// answer-service/src/main/java/com/djy/quiz/feign/QuestionServiceClient.java
package com.djy.quiz.feign;
import java.util.List;

import com.djy.quiz.fallback.QuestionServiceFallback;
import com.djy.quiz.pojo.dto.QuestionDTO;
import com.djy.quiz.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "question-service",
        path = "/api/quiz",
        fallback = QuestionServiceFallback.class  // 指定Fallback类
)
public interface QuestionServiceClient {

    @GetMapping("/questions/{id}")
    Result<QuestionDTO> getQuestionById(@PathVariable("id") Integer id);

    @GetMapping("/questions")
    Result<List<QuestionDTO>> getAllQuestions();
}

