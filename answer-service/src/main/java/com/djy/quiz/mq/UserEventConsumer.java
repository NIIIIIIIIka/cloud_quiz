package com.djy.quiz.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 用户事件消息消费者
 * 接收来自用户服务的事件消息
 */
@Slf4j
@Component
public class UserEventConsumer {

  /**
   * 监听用户注册事件
   * 可以在这里执行一些初始化操作，比如为新用户创建答题统计记录
   */
  @RabbitListener(queues = "user.register.queue")
  public void handleUserRegisterEvent(Map<String, Object> event) {
    try {
      Long userId = ((Number) event.get("userId")).longValue();
      String userName = (String) event.get("userName");
      String eventType = (String) event.get("eventType");

      log.info("收到用户注册事件: userId={}, userName={}, eventType={}", userId, userName, eventType);

      // 这里可以执行业务逻辑，比如：
      // 1. 为新用户初始化答题统计
      // 2. 发送欢迎消息
      // 3. 记录用户行为日志

      log.info("用户注册事件处理完成: userId={}", userId);
    } catch (Exception e) {
      log.error("处理用户注册事件失败: {}", e.getMessage(), e);
    }
  }

  /**
   * 监听用户登录事件
   */
  @RabbitListener(queues = "user.login.queue")
  public void handleUserLoginEvent(Map<String, Object> event) {
    try {
      Long userId = ((Number) event.get("userId")).longValue();
      String userName = (String) event.get("userName");

      log.info("收到用户登录事件: userId={}, userName={}", userId, userName);

      // 这里可以执行业务逻辑，比如：
      // 1. 更新用户最后登录时间
      // 2. 加载用户的答题进度到缓存

    } catch (Exception e) {
      log.error("处理用户登录事件失败: {}", e.getMessage(), e);
    }
  }
}
