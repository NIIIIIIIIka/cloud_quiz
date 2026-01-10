package com.djy.quiz.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 答题事件消费者
 * 接收答题事件，更新题目统计信息（如：答题次数、正确率等）
 */
@Slf4j
@Component
public class AnswerEventConsumer {

  private final RedisTemplate<String, Object> redisTemplate;

  private static final String QUESTION_STATS_PREFIX = "question:stats:";

  public AnswerEventConsumer(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * 监听答题提交事件
   * 更新题目的答题统计信息
   */
  @RabbitListener(queues = "answer.stats.queue")
  public void handleAnswerSubmitEvent(Map<String, Object> event) {
    try {
      Long questionId = ((Number) event.get("questionId")).longValue();
      Boolean isCorrect = (Boolean) event.get("isCorrect");

      log.info("收到答题事件: questionId={}, isCorrect={}", questionId, isCorrect);

      // 更新 Redis 中的题目统计
      String statsKey = QUESTION_STATS_PREFIX + questionId;

      // 增加答题次数
      redisTemplate.opsForHash().increment(statsKey, "totalCount", 1);

      // 如果答对，增加正确次数
      if (Boolean.TRUE.equals(isCorrect)) {
        redisTemplate.opsForHash().increment(statsKey, "correctCount", 1);
      }

      log.info("题目统计已更新: questionId={}", questionId);

    } catch (Exception e) {
      log.error("处理答题事件失败: {}", e.getMessage(), e);
    }
  }
}
