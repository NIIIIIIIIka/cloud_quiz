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
 * 用于监听答题事件，更新题目统计信息
 */
@Configuration
public class RabbitMQConfig {

  // 答题服务的交换机和队列（用于接收答题事件）
  public static final String ANSWER_EXCHANGE = "answer.exchange";
  public static final String ANSWER_STATS_QUEUE = "answer.stats.queue";
  public static final String ANSWER_SUBMIT_ROUTING_KEY = "answer.submit";

  /**
   * 答题统计队列（题目服务用于接收答题事件更新统计）
   */
  @Bean
  public Queue answerStatsQueue() {
    return QueueBuilder.durable(ANSWER_STATS_QUEUE).build();
  }

  /**
   * 答题事件交换机
   */
  @Bean
  public TopicExchange answerExchange() {
    return new TopicExchange(ANSWER_EXCHANGE, true, false);
  }

  /**
   * 绑定统计队列到答题交换机
   */
  @Bean
  public Binding statsBinding() {
    return BindingBuilder.bind(answerStatsQueue()).to(answerExchange()).with(ANSWER_SUBMIT_ROUTING_KEY);
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
