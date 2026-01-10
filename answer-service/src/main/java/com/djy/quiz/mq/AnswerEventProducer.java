package com.djy.quiz.mq;

import com.djy.quiz.config.RabbitMQConfig;
import com.djy.quiz.pojo.dto.AnswerEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 答题事件消息生产者
 */
@Slf4j
@Component
public class AnswerEventProducer {

  private final RabbitTemplate rabbitTemplate;

  public AnswerEventProducer(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * 发送答题提交事件
   */
  public void sendAnswerSubmitEvent(Long userId, Long questionId, Boolean isCorrect, Integer score) {
    AnswerEventDTO event = new AnswerEventDTO();
    event.setUserId(userId);
    event.setQuestionId(questionId);
    event.setIsCorrect(isCorrect);
    event.setScore(score);
    event.setEventTime(LocalDateTime.now());

    log.info("发送答题提交事件: userId={}, questionId={}, isCorrect={}", userId, questionId, isCorrect);
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.ANSWER_EXCHANGE,
        RabbitMQConfig.ANSWER_SUBMIT_ROUTING_KEY,
        event);
  }
}
