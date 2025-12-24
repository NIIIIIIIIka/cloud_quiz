// answer-service/src/main/java/com/djy/quiz/feign/UserServiceClient.java
package com.djy.quiz.feign;

import com.djy.quiz.fallback.UserServiceFallback;
import com.djy.quiz.pojo.vo.UserVO;
import com.djy.quiz.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service",
        path = "/api/user",
        fallback = UserServiceFallback.class,
        fallbackFactory = UserServiceFallback.class)  // 服务名和路径前缀
public interface UserServiceClient {

    @GetMapping("/users/{id}")
    Result<UserVO> getUserById(@PathVariable("id") Long id);

}

