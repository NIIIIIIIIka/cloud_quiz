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
 * 用于接收用户事件和发送答题事件
 */
@Configuration
public class RabbitMQConfig {

  // 用户服务的交换机和队列（用于接收用户事件）
  public static final String USER_EXCHANGE = "user.exchange";
  public static final String USER_REGISTER_QUEUE = "user.register.queue";
  public static final String USER_LOGIN_QUEUE = "user.login.queue";
  public static final String USER_REGISTER_ROUTING_KEY = "user.register";
  public static final String USER_LOGIN_ROUTING_KEY = "user.login";

  // 答题服务的交换机和队列（用于发送答题事件）
  public static final String ANSWER_EXCHANGE = "answer.exchange";
  public static final String ANSWER_SUBMIT_QUEUE = "answer.submit.queue";
  public static final String ANSWER_SUBMIT_ROUTING_KEY = "answer.submit";

  // ========== 用户服务队列声明（消费端也需要声明队列） ==========
  @Bean
  public TopicExchange userExchange() {
    return new TopicExchange(USER_EXCHANGE, true, false);
  }

  @Bean
  public Queue userRegisterQueue() {
    return QueueBuilder.durable(USER_REGISTER_QUEUE).build();
  }

  @Bean
  public Queue userLoginQueue() {
    return QueueBuilder.durable(USER_LOGIN_QUEUE).build();
  }

  @Bean
  public Binding userRegisterBinding() {
    return BindingBuilder.bind(userRegisterQueue()).to(userExchange()).with(USER_REGISTER_ROUTING_KEY);
  }

  @Bean
  public Binding userLoginBinding() {
    return BindingBuilder.bind(userLoginQueue()).to(userExchange()).with(USER_LOGIN_ROUTING_KEY);
  }

  // ========== 答题服务队列声明 ==========
  @Bean
  public TopicExchange answerExchange() {
    return new TopicExchange(ANSWER_EXCHANGE, true, false);
  }

  @Bean
  public Queue answerSubmitQueue() {
    return QueueBuilder.durable(ANSWER_SUBMIT_QUEUE).build();
  }

  @Bean
  public Binding answerBinding() {
    return BindingBuilder.bind(answerSubmitQueue()).to(answerExchange()).with(ANSWER_SUBMIT_ROUTING_KEY);
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter());
    return rabbitTemplate;
  }
}
