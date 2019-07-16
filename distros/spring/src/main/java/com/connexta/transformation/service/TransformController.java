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

import com.connexta.transformation.rest.models.TransformRequest;
import com.connexta.transformation.rest.springboot.TransformApi;
import com.google.common.base.Preconditions;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation of the Transformation RESTful service. This is the main entry point for all HTTP
 * requests.
 */
@RestController
@CrossOrigin(origins = "*")
public class TransformController implements TransformApi {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransformController.class);

  private static final String ACCEPT_VERSION = "Accept-Version";

  private final RestTemplate restTemplate;

  private final String lookupServiceUrl;

  public TransformController(
      RestTemplate restTemplate, @Qualifier("lookupServiceUrl") String lookupServiceUrl) {
    Preconditions.checkNotNull(restTemplate, "Rest Template cannot be null.");
    Preconditions.checkNotNull(lookupServiceUrl, "Lookup Service Url cannot be null.");
    this.restTemplate = restTemplate;
    this.lookupServiceUrl = lookupServiceUrl;
  }

  /**
   * Handles application/json POST transform requests to the /transform context. This method
   * handles: - Validating the message - Forwarding the request to the service lookup service.
   * Returns an HTTP Status Code of 202 Accepted on success; otherwise, an error status code.
   */
  @RequestMapping(
      value = "/transform",
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE},
      method = RequestMethod.POST)
  public ResponseEntity<Void> transform(
      @RequestHeader(ACCEPT_VERSION) String acceptVersion,
      @Valid @RequestBody TransformRequest transformRequest) {
    LOGGER.debug("{}: {}", ACCEPT_VERSION, acceptVersion);
    forwardRequest(transformRequest);
    return ResponseEntity.accepted().build();
  }

  private void forwardRequest(TransformRequest transformRequest) {
    LOGGER.debug("Forwarding request to: {}", lookupServiceUrl);
    ResponseEntity<String> response =
        restTemplate.postForEntity(lookupServiceUrl, transformRequest, String.class);
    LOGGER.debug(
        "HTTP response status code from lookup service: {}", response.getStatusCodeValue());
  }
}
