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

import com.connexta.transformation.commons.api.TransformationManager;
import com.connexta.transformation.commons.api.exceptions.TransformationException;
import com.connexta.transformation.rest.models.ErrorResponse;
import com.connexta.transformation.rest.models.TransformRequest;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
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
public class TransformController {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransformController.class);

  private static final String ACCEPT_VERSION = "Accept-Version";

  private final RestTemplate restTemplate;

  private final String lookupServiceUrl;

  public TransformationManager transformationManager;

  @Autowired
  public TransformController(
      RestTemplateBuilder builder,
      @Qualifier("lookupServiceUrl") String lookupServiceUrl,
      TransformationManager transformManager) {
    this(builder.build(), lookupServiceUrl);
    this.transformationManager = transformManager;
  }

  @VisibleForTesting
  TransformController(
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
  @ApiOperation(
      value = "Submit transformation request",
      nickname = "transform",
      notes =
          "A request to transform resources into discovery metadata and other supporting products.",
      tags = {"transform"})
  @ApiResponses({
    @ApiResponse(
        code = 201,
        message =
            "The transformation request was accepted for processing. The URI for polling the status is returned in the Location header of the response. "),
    @ApiResponse(
        code = 400,
        message =
            "The client message could not be understood by the server due to invalid format or syntax.",
        response = ErrorResponse.class),
    @ApiResponse(
        code = 401,
        message = "The client could not be authenticated.",
        response = ErrorResponse.class),
    @ApiResponse(
        code = 403,
        message = "The client is not authorized.",
        response = ErrorResponse.class),
    @ApiResponse(
        code = 500,
        message =
            "The server encountered an unexpected condition that prevented it from fulfilling the request.",
        response = ErrorResponse.class),
    @ApiResponse(
        code = 501,
        message =
            "The requested API version is not supported and therefore not implemented. Possible codes reported are: - 501001 - Unable to parse *Accept-Version* - 501002 - The provided major version is no longer supported - 501003 - The provided major version is not yet supported by the server - 501004 - The provided minor version is not yet supported by the server ",
        response = ErrorResponse.class),
    @ApiResponse(
        code = 200,
        message = "Any other possible errors not currently known.",
        response = ErrorResponse.class)
  })
  @RequestMapping(
      value = {"/transform"},
      produces = {"application/json"},
      consumes = {"application/json"},
      method = {RequestMethod.POST})
  public ResponseEntity<Void> transform(
      @ApiParam(
              value =
                  "The API version used by the client to produce the REST message. The client must accept responses marked with any minor versions after this one. This implies that all future minor versions of the message are backward compatible with all previous minor versions of the same message. ",
              required = true)
          @RequestHeader(value = "Accept-Version", required = true)
          String acceptVersion,
      @ApiParam(
              value =
                  "A request to transform a file into discovery metadata and other supporting products.",
              required = true)
          @Valid
          @RequestBody
          TransformRequest transformRequest) {
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

  /**
   * Handles application/json DELETE transform requests to the /transform context. This method
   * handles: - Deleting the given transformId. - If the transformId is not there, a
   * TransformationNotFoundException gets thrown. Returns an HTTP Status Code of 204 No Content on
   * success; otherwise, an error status code.
   */
  @ApiOperation(
      value = "Delete the transformation request",
      nickname = "delete",
      notes =
          "After retrieving the output of the transformation, the client should call this endpoint to remove the request. ",
      tags = {"transform"})
  @ApiResponses({
    @ApiResponse(code = 204, message = "The transformation request was successfully deleted."),
    @ApiResponse(
        code = 400,
        message =
            "The client message could not be understood by the server due to invalid format or syntax.",
        response = ErrorResponse.class),
    @ApiResponse(
        code = 401,
        message = "The client could not be authenticated.",
        response = ErrorResponse.class),
    @ApiResponse(
        code = 403,
        message = "The client is not authorized.",
        response = ErrorResponse.class),
    @ApiResponse(
        code = 404,
        message = "The transform request could not be found.",
        response = ErrorResponse.class),
    @ApiResponse(
        code = 501,
        message =
            "The requested API version is not supported and therefore not implemented. Possible codes reported are: - 501001 - Unable to parse *Accept-Version* - 501002 - The provided major version is no longer supported - 501003 - The provided major version is not yet supported by the server - 501004 - The provided minor version is not yet supported by the server ",
        response = ErrorResponse.class),
    @ApiResponse(
        code = 200,
        message = "Any other possible errors not currently known.",
        response = ErrorResponse.class)
  })
  @RequestMapping(
      value = {"/transform/{TransformId}"},
      produces = {"application/json"},
      method = {RequestMethod.DELETE})
  public ResponseEntity<Void> delete(
      @ApiParam(value = "The ID of the transform request. ", required = true)
          @PathVariable("TransformId")
          String transformId)
      throws TransformationException {
    transformationManager.delete(transformId);
    return ResponseEntity.noContent().build();
  }
}
