package com.djy.quiz.service.impl;

import com.djy.quiz.mapper.AnswerHistoryMapper;
import com.djy.quiz.mq.AnswerEventProducer;
import com.djy.quiz.pojo.model.AnswerHistory;
import com.djy.quiz.service.AnswerHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 答题历史服务实现类
 * 集成技术：
 * - Redis 缓存（答题历史缓存）
 * - RabbitMQ（答题事件异步通知）
 * - Seata（分布式事务）
 * - Micrometer Tracing（链路追踪自动集成）
 */
@Slf4j
@Service
public class AnswerHistoryServiceImpl implements AnswerHistoryService {

  private final AnswerHistoryMapper answerHistoryMapper;
  private final RedisTemplate<String, Object> redisTemplate;
  private final AnswerEventProducer answerEventProducer;

  private static final String ANSWER_CACHE_PREFIX = "answer:history:";
  private static final String USER_ANSWERS_CACHE_PREFIX = "answer:user:";
  private static final long CACHE_TTL_MINUTES = 10;

  public AnswerHistoryServiceImpl(AnswerHistoryMapper answerHistoryMapper,
      RedisTemplate<String, Object> redisTemplate,
      AnswerEventProducer answerEventProducer) {
    this.answerHistoryMapper = answerHistoryMapper;
    this.redisTemplate = redisTemplate;
    this.answerEventProducer = answerEventProducer;
  }

  /**
   * 添加答题记录
   */
  @Override
  public void add(AnswerHistory h) {
    log.info("添加答题记录开始: userId={}, questionId={}", h.getUserId(), h.getQuestionId());

    answerHistoryMapper.insert(h);

    // 清除用户答题列表缓存
    String userCacheKey = USER_ANSWERS_CACHE_PREFIX + h.getUserId();
    redisTemplate.delete(userCacheKey);

    // 发送答题事件到 RabbitMQ
    try {
      // 答题正确得10分，否则0分
      int score = h.getIsCorrect() == 1 ? 10 : 0;
      answerEventProducer.sendAnswerSubmitEvent(
          h.getUserId(),
          h.getQuestionId().longValue(),
          h.getIsCorrect() == 1,
          score);
      log.info("答题事件已发送: userId={}, questionId={}", h.getUserId(), h.getQuestionId());
    } catch (Exception e) {
      log.warn("发送答题事件失败: {}", e.getMessage());
    }

    log.info("添加答题记录完成: historyId={}", h.getAnswerHistoryId());
  }

  @Override
  @CacheEvict(value = "answerHistory", key = "#p0.answerHistoryId")
  public void update(AnswerHistory h) {
    log.info("更新答题记录: historyId={}", h.getAnswerHistoryId());
    answerHistoryMapper.update(h);

    // 清除相关缓存
    String cacheKey = ANSWER_CACHE_PREFIX + h.getAnswerHistoryId();
    redisTemplate.delete(cacheKey);

    String userCacheKey = USER_ANSWERS_CACHE_PREFIX + h.getUserId();
    redisTemplate.delete(userCacheKey);
  }

  @Override
  @CacheEvict(value = "answerHistory", key = "#p0")
  public void delete(Long answerHistoryId) {
    log.info("删除答题记录: historyId={}", answerHistoryId);

    // 先获取记录以清除用户缓存
    AnswerHistory h = answerHistoryMapper.findById(answerHistoryId);

    answerHistoryMapper.hardDelete(answerHistoryId);

    // 清除缓存
    String cacheKey = ANSWER_CACHE_PREFIX + answerHistoryId;
    redisTemplate.delete(cacheKey);

    if (h != null) {
      String userCacheKey = USER_ANSWERS_CACHE_PREFIX + h.getUserId();
      redisTemplate.delete(userCacheKey);
    }
  }

  @Override
  @Cacheable(value = "answerHistory", key = "#p0")
  public AnswerHistory getById(Long answerHistoryId) {
    log.info("查询答题记录: historyId={}", answerHistoryId);

    // 先从 Redis 缓存获取
    String cacheKey = ANSWER_CACHE_PREFIX + answerHistoryId;
    Object cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
      log.info("从 Redis 缓存获取答题记录: historyId={}", answerHistoryId);
      return (AnswerHistory) cached;
    }

    // 缓存未命中，从数据库查询
    AnswerHistory h = answerHistoryMapper.findById(answerHistoryId);

    if (h != null) {
      // 写入缓存
      redisTemplate.opsForValue().set(cacheKey, h, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
      log.info("答题记录已写入 Redis 缓存: historyId={}", answerHistoryId);
    }

    return h;
  }

  @Override
  public List<AnswerHistory> listByUser(Long userId) {
    log.info("查询用户答题记录列表: userId={}", userId);

    // 先从 Redis 缓存获取
    String cacheKey = USER_ANSWERS_CACHE_PREFIX + userId;
    Object cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
      log.info("从 Redis 缓存获取用户答题记录: userId={}", userId);
      return (List<AnswerHistory>) cached;
    }

    // 缓存未命中，从数据库查询
    List<AnswerHistory> list = answerHistoryMapper.listByUser(userId);

    // 写入缓存
    redisTemplate.opsForValue().set(cacheKey, list, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    log.info("用户答题记录已写入 Redis 缓存: userId={}, count={}", userId, list.size());

    return list;
  }

  @Override
  public List<AnswerHistory> listAll() {
    return answerHistoryMapper.listAll();
  }
}
