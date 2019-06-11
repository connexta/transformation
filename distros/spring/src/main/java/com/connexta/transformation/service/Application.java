/**
 * Copyright (c) Connexta
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package com.connexta.transformation.service;

import com.connexta.transformation.TransformationService;
import com.connexta.transformation.service.TransformationServiceImpl.DeliveryConfirmCallback;
import com.connexta.transformation.service.TransformationServiceImpl.DeliveryReturnCallback;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Main class for the Transformation implementation application. This class also is the
 * configuration class for the application.
 */
@SpringBootApplication
public class Application {

  private static final int MAX_PAYLOAD_LENGTH = 5000;

  private static final String INBOUND_REQUEST_PREFIX = "Inbound Request: ";

  @Value("${spring.rabbitmq.host}")
  private String messageBrokerHost;

  @Value("${spring.rabbitmq.port}")
  private int messageBrokerPort;

  @Value("${spring.rabbitmq.virtualHost}")
  private String virtualHost;

  @Value("${spring.rabbitmq.user}")
  private String user;

  @Value("${spring.rabbitmq.password}")
  private String password;

  @Value("${spring.rabbitmq.exchange}")
  private String exchange;

  @Value("${spring.rabbitmq.routingKey}")
  private String routingKey;

  @Bean
  public CommonsRequestLoggingFilter requestLoggingFilter() {
    final CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
    loggingFilter.setIncludeClientInfo(true);
    loggingFilter.setIncludeQueryString(true);
    loggingFilter.setIncludePayload(true);
    loggingFilter.setIncludeHeaders(true);
    loggingFilter.setAfterMessagePrefix(INBOUND_REQUEST_PREFIX);
    loggingFilter.setMaxPayloadLength(MAX_PAYLOAD_LENGTH);
    return loggingFilter;
  }

  @Bean(name = "exchange")
  public String getExchange() {
    return exchange;
  }

  @Bean(name = "routingKey")
  public String getRoutingKey() {
    return routingKey;
  }

  @Bean
  public TransformationService transformationService(
      RabbitTemplate rabbitTemplate, String exchange, String routingKey) {
    return new TransformationServiceImpl(rabbitTemplate, exchange, routingKey);
  }

  @Bean
  public TransformController transformController(TransformationService transformationService) {
    return new TransformController(transformationService);
  }

  @Bean
  public ConnectionFactory connectionFactory() {
    final CachingConnectionFactory connectionFactory =
        new CachingConnectionFactory(messageBrokerHost, messageBrokerPort);
    connectionFactory.setUsername(user);
    connectionFactory.setPassword(password);
    connectionFactory.setVirtualHost(virtualHost);
    connectionFactory.setPublisherReturns(true);
    connectionFactory.setPublisherConfirms(true);
    return connectionFactory;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory,
      DeliveryConfirmCallback deliveryConfirmCallback,
      DeliveryReturnCallback deliveryReturnCallback,
      Jackson2JsonMessageConverter jackson2MessageConverter) {
    final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setExchange(exchange);
    rabbitTemplate.setRoutingKey(routingKey);
    rabbitTemplate.setMessageConverter(jackson2MessageConverter);
    rabbitTemplate.setMandatory(true);
    rabbitTemplate.setConfirmCallback(deliveryConfirmCallback);
    rabbitTemplate.setReturnCallback(deliveryReturnCallback);
    return rabbitTemplate;
  }

  @Bean
  public Jackson2JsonMessageConverter jackson2MessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public DeliveryConfirmCallback deliveryConfirmCallback() {
    return new DeliveryConfirmCallback();
  }

  @Bean
  public DeliveryReturnCallback deliveryReturnCallback() {
    return new DeliveryReturnCallback();
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
