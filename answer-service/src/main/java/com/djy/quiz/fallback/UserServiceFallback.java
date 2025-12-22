package com.djy.quiz.fallback;

import com.djy.quiz.feign.UserServiceClient;
import com.djy.quiz.pojo.vo.UserVO;
import com.djy.quiz.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class UserServiceFallback implements UserServiceClient {
    @Override
    public Result<UserVO> getUserById(Long id) {
        log.error("UserService熔断: 获取用户[id={}]失败", id);

        // 方案1: 返回缓存数据（如果有）
        // 方案2: 返回默认用户
        UserVO defaultUser = new UserVO();
        defaultUser.setUserId(id);
        defaultUser.setUserName("【降级用户】");
        defaultUser.setUserRole(0);

        return Result.ok(defaultUser);
    }
//    @Override
//    public Result<UserVO> getCurrentUserInfo() {
//        log.warn("UserService降级: 无法获取当前用户信息");
//        return Result.error(503, "用户服务暂时不可用");
//    }
}
