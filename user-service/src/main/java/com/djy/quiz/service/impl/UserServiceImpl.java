package com.djy.quiz.service.impl;

import com.djy.quiz.constant.RoleConstant;
import com.djy.quiz.mapper.UserMapper;
import com.djy.quiz.mq.UserEventProducer;
import com.djy.quiz.pojo.dto.UserLoginDTO;
import com.djy.quiz.pojo.dto.UserRegisterDTO;
import com.djy.quiz.pojo.model.User;
import com.djy.quiz.pojo.vo.UserVO;
import com.djy.quiz.service.UserService;
import com.djy.quiz.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 集成技术：
 * - Redis 缓存（用户信息缓存）
 * - RabbitMQ（用户事件异步通知）
 * - Micrometer Tracing（链路追踪自动集成）
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

  private final UserMapper userMapper;
  private final BCryptPasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final RedisTemplate<String, Object> redisTemplate;
  private final UserEventProducer userEventProducer;

  private static final String USER_CACHE_PREFIX = "user:";
  private static final long CACHE_TTL_MINUTES = 30;

  public UserServiceImpl(UserMapper userMapper,
      BCryptPasswordEncoder passwordEncoder,
      JwtUtil jwtUtil,
      RedisTemplate<String, Object> redisTemplate,
      UserEventProducer userEventProducer) {
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
    this.redisTemplate = redisTemplate;
    this.userEventProducer = userEventProducer;
  }

  @Override
  public void register(UserRegisterDTO dto) {
    log.info("用户注册开始: userName={}", dto.getUserName());

    if (userMapper.findByUserName(dto.getUserName()) != null) {
      throw new IllegalArgumentException("用户名已存在");
    }
    User user = new User();
    user.setUserName(dto.getUserName());
    user.setUserPassword(passwordEncoder.encode(dto.getPassword()));
    user.setUserRole(RoleConstant.USER);
    userMapper.insert(user);

    // 发送用户注册事件到 RabbitMQ
    try {
      userEventProducer.sendRegisterEvent(user.getUserId(), user.getUserName());
      log.info("用户注册事件已发送: userId={}", user.getUserId());
    } catch (Exception e) {
      log.warn("发送用户注册事件失败: {}", e.getMessage());
    }
  }

  @Override
  public String login(UserLoginDTO dto) {
    log.info("用户登录开始: userName={}", dto.getUserName());

    User user = userMapper.findByUserName(dto.getUserName());
    if (user == null) {
      throw new IllegalArgumentException("用户不存在");
    }
    if (!passwordEncoder.matches(dto.getPassword(), user.getUserPassword())) {
      throw new IllegalArgumentException("密码错误");
    }

    String token = jwtUtil.generateToken(user.getUserId(), user.getUserName(), user.getUserRole());

    // 缓存用户信息到 Redis
    String cacheKey = USER_CACHE_PREFIX + user.getUserId();
    redisTemplate.opsForValue().set(cacheKey, toVO(user), CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    log.info("用户信息已缓存到 Redis: key={}", cacheKey);

    // 发送用户登录事件到 RabbitMQ
    try {
      userEventProducer.sendLoginEvent(user.getUserId(), user.getUserName());
      log.info("用户登录事件已发送: userId={}", user.getUserId());
    } catch (Exception e) {
      log.warn("发送用户登录事件失败: {}", e.getMessage());
    }

    return token;
  }

  @Override
  @Cacheable(value = "users", key = "#p0")
  public UserVO getById(Long userId) {
    log.info("查询用户信息: userId={}", userId);

    // 先从 Redis 缓存获取
    String cacheKey = USER_CACHE_PREFIX + userId;
    Object cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
      log.info("从 Redis 缓存获取用户信息: userId={}", userId);
      return (UserVO) cached;
    }

    // 缓存未命中，从数据库查询
    User user = userMapper.findById(userId);
    if (user == null) {
      throw new IllegalArgumentException("用户不存在");
    }

    UserVO vo = toVO(user);
    // 写入缓存
    redisTemplate.opsForValue().set(cacheKey, vo, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    log.info("用户信息已写入 Redis 缓存: userId={}", userId);

    return vo;
  }

  @Override
  public List<UserVO> listAll() {
    return userMapper.listAll().stream().map(this::toVO).collect(Collectors.toList());
  }

  @Override
  @CacheEvict(value = "users", key = "#p0.userId")
  public void update(User user) {
    log.info("更新用户信息: userId={}", user.getUserId());

    if (user.getUserPassword() != null && !user.getUserPassword().isBlank()) {
      user.setUserPassword(passwordEncoder.encode(user.getUserPassword()));
    }
    userMapper.update(user);

    // 清除 Redis 缓存
    String cacheKey = USER_CACHE_PREFIX + user.getUserId();
    redisTemplate.delete(cacheKey);
    log.info("已清除用户 Redis 缓存: userId={}", user.getUserId());
  }

  @Override
  @CacheEvict(value = "users", key = "#p0")
  public void delete(Long userId) {
    log.info("删除用户: userId={}", userId);
    userMapper.softDelete(userId);

    // 清除 Redis 缓存
    String cacheKey = USER_CACHE_PREFIX + userId;
    redisTemplate.delete(cacheKey);
  }

  private UserVO toVO(User u) {
    UserVO vo = new UserVO();
    vo.setUserId(u.getUserId());
    vo.setUserName(u.getUserName());
    vo.setUserRole(u.getUserRole());
    return vo;
  }
}
