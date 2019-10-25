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

import static com.connexta.transformation.service.PollResponseCreator.convertToPollResponse;

import com.connexta.spring.error.DetailedResponseStatusException;
import com.connexta.transformation.commons.api.Transformation;
import com.connexta.transformation.commons.api.TransformationManager;
import com.connexta.transformation.commons.api.exceptions.TransformationException;
import com.connexta.transformation.commons.api.exceptions.TransformationNotFoundException;
import com.connexta.transformation.rest.models.TransformationPollResponse;
import com.connexta.transformation.rest.spring.TransformApiPoll;
import com.connexta.transformation.service.Exceptions.UnknownStateException;
import io.swagger.annotations.ApiParam;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** A rest controller for transformation status calls */
@RestController
public class TransformStatusController implements TransformApiPoll {

  private final TransformationManager transformManager;

  public TransformStatusController(TransformationManager transformManager) {
    this.transformManager = transformManager;
  }

  /**
   * For more documentation see {@link TransformApiPoll#poll(String)}. Retrieves a transformation
   * with the given ID and creates a poll response from its status information.
   *
   * @param transformId The ID of the transformation to retrieve
   * @return A {@link TransformationPollResponse} containing the status information of the
   *     transformation
   * @throws TransformationNotFoundException if no transformation with the given ID could be found
   * @throws TransformationException if an error occurs while retrieving the transformation
   * @throws DetailedResponseStatusException if the state of the transformation could not be read
   */
  @Override
  public ResponseEntity<TransformationPollResponse> poll(
      @ApiParam(value = "The ID of the transform request. ", required = true)
          @PathVariable("TransformId")
          String transformId)
      throws TransformationException {
    final Transformation transformation = transformManager.get(transformId);
    final URI requestUri = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
    final TransformationPollResponse pollResponse;

    try {
      pollResponse = convertToPollResponse(transformation, requestUri);
    } catch (UnknownStateException e) {
      // TODO: include Retry-After header when the transformation-api supports it.
      throw new DetailedResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
    }

    return new ResponseEntity<>(pollResponse, HttpStatus.OK);
  }
}
