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
import com.connexta.transformation.rest.models.TransformRequest;
import com.connexta.transformation.rest.models.TransformResponse;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * {@code TransformationServiceImpl} handles the business logic for forwarding the transform request
 * to the request queue for further processing.
 */
@Service
public class TransformationServiceImpl implements TransformationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransformationServiceImpl.class);

  private static final String TRANSFORM_RESPONSE_TEMPLATE =
      "The request with ID %s has been accepted for processing.";

  private final String exchange;

  private final String routingKey;

  private final RabbitTemplate rabbitTemplate;

  /**
   * Constructor which sets up communication with RabbitMQ
   *
   * @param rabbitTemplate helper class that handles RabbitMQ messaging
   * @param exchange the message routing agent
   * @param routingKey destination queue name
   */
  public TransformationServiceImpl(
      RabbitTemplate rabbitTemplate, String exchange, String routingKey) {
    Preconditions.checkNotNull(rabbitTemplate, "Rabbit Template cannot be null.");
    Preconditions.checkNotNull(exchange, "Exchange cannot be null.");
    Preconditions.checkNotNull(routingKey, "Routing Key cannot be null.");
    this.rabbitTemplate = rabbitTemplate;
    this.exchange = exchange;
    this.routingKey = routingKey;
  }

  /**
   * Forwards the transform request to the request queue.
   *
   * @param transformRequest request to transform an input
   * @return response stating that a request has been accepted for processing
   */
  @Override
  public TransformResponse transform(TransformRequest transformRequest) {
    try {
      forwardTransformRequest(transformRequest, new CorrelationData(transformRequest.getId()));
    } catch (AmqpIOException e) {
      failRequest(transformRequest, e);
    }
    return createTransformResponse(transformRequest);
  }

  /**
   * @param transformRequest request to transform an input
   * @param correlationData transform request ID used for correlation
   */
  private void forwardTransformRequest(
      TransformRequest transformRequest, CorrelationData correlationData) {
    rabbitTemplate.convertAndSend(exchange, routingKey, transformRequest, correlationData);
  }

  private void failRequest(TransformRequest transformRequest, Exception e) {
    // TODO: Should we store failed requests for later processing, or should return an error
    // response back to the caller stating that the request failed processing?
    LOGGER.error(
        String.format("Transformation request with ID %s failed.", transformRequest.getId()), e);
  }

  private TransformResponse createTransformResponse(TransformRequest transformRequest) {
    final TransformResponse response = new TransformResponse();
    response.setId(transformRequest.getId());
    response.setMessage(String.format(TRANSFORM_RESPONSE_TEMPLATE, transformRequest.getId()));
    return response;
  }

  static class DeliveryConfirmCallback implements RabbitTemplate.ConfirmCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryConfirmCallback.class);

    private static final String LOG_TEMPLATE =
        System.lineSeparator()
            + "================"
            + System.lineSeparator()
            + "Correlation Data: %s"
            + System.lineSeparator()
            + "Ack: %s"
            + System.lineSeparator()
            + "Cause: %s"
            + System.lineSeparator()
            + "================"
            + System.lineSeparator();

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
      LOGGER.debug(String.format(LOG_TEMPLATE, correlationData, ack, cause));
    }
  }

  static class DeliveryReturnCallback implements RabbitTemplate.ReturnCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveryReturnCallback.class);

    private static final String LOG_TEMPLATE =
        System.lineSeparator()
            + "================"
            + System.lineSeparator()
            + "Message: %s"
            + System.lineSeparator()
            + "Reply Code: %d"
            + System.lineSeparator()
            + "Reply Text: %s"
            + System.lineSeparator()
            + "Exchange: %s"
            + System.lineSeparator()
            + "Routing Key: %s"
            + System.lineSeparator()
            + "================"
            + System.lineSeparator();

    @Override
    public void returnedMessage(
        Message message, int replyCode, String replyText, String exchange, String routingKey) {
      LOGGER.debug(
          String.format(LOG_TEMPLATE, message, replyCode, replyText, exchange, routingKey));
    }
  }
}
