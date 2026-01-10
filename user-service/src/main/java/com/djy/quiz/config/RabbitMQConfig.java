package com.djy.quiz.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 用于用户注册事件的异步通知
 */
@Configuration
public class RabbitMQConfig {

  // 交换机名称
  public static final String USER_EXCHANGE = "user.exchange";

  // 队列名称
  public static final String USER_REGISTER_QUEUE = "user.register.queue";
  public static final String USER_LOGIN_QUEUE = "user.login.queue";

  // 路由键
  public static final String USER_REGISTER_ROUTING_KEY = "user.register";
  public static final String USER_LOGIN_ROUTING_KEY = "user.login";

  /**
   * 用户事件交换机（Topic 类型）
   */
  @Bean
  public TopicExchange userExchange() {
    return new TopicExchange(USER_EXCHANGE, true, false);
  }

  /**
   * 用户注册队列
   */
  @Bean
  public Queue userRegisterQueue() {
    return QueueBuilder.durable(USER_REGISTER_QUEUE).build();
  }

  /**
   * 用户登录队列
   */
  @Bean
  public Queue userLoginQueue() {
    return QueueBuilder.durable(USER_LOGIN_QUEUE).build();
  }

  /**
   * 绑定注册队列到交换机
   */
  @Bean
  public Binding registerBinding() {
    return BindingBuilder.bind(userRegisterQueue()).to(userExchange()).with(USER_REGISTER_ROUTING_KEY);
  }

  /**
   * 绑定登录队列到交换机
   */
  @Bean
  public Binding loginBinding() {
    return BindingBuilder.bind(userLoginQueue()).to(userExchange()).with(USER_LOGIN_ROUTING_KEY);
  }

  /**
   * 消息转换器（使用 JSON 格式）
   */
  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  /**
   * RabbitTemplate 配置
   */
  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter());
    return rabbitTemplate;
  }
}
