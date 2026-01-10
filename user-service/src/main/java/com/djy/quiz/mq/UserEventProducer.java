package com.djy.quiz.mq;

import com.djy.quiz.config.RabbitMQConfig;
import com.djy.quiz.pojo.dto.UserEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 用户事件消息生产者
 * 使用 RabbitMQ 发送用户相关事件
 */
@Slf4j
@Component
public class UserEventProducer {

  private final RabbitTemplate rabbitTemplate;

  public UserEventProducer(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * 发送用户注册事件
   */
  public void sendRegisterEvent(Long userId, String userName) {
    UserEventDTO event = new UserEventDTO();
    event.setUserId(userId);
    event.setUserName(userName);
    event.setEventType("REGISTER");
    event.setEventTime(LocalDateTime.now());

    log.info("发送用户注册事件: userId={}, userName={}", userId, userName);
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.USER_EXCHANGE,
        RabbitMQConfig.USER_REGISTER_ROUTING_KEY,
        event);
  }

  /**
   * 发送用户登录事件
   */
  public void sendLoginEvent(Long userId, String userName) {
    UserEventDTO event = new UserEventDTO();
    event.setUserId(userId);
    event.setUserName(userName);
    event.setEventType("LOGIN");
    event.setEventTime(LocalDateTime.now());

    log.info("发送用户登录事件: userId={}, userName={}", userId, userName);
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.USER_EXCHANGE,
        RabbitMQConfig.USER_LOGIN_ROUTING_KEY,
        event);
  }
}
