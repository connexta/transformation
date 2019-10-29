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
import com.connexta.transformation.commons.api.exceptions.TransformationNotFoundException;
import com.connexta.transformation.rest.spring.TransformApiDelete;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implementation of the Transformation RESTful Delete service. This is the main entry point for all
 * HTTP Delete requests.
 */
@RestController
@CrossOrigin(origins = "*")
public class DeleteController implements TransformApiDelete {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeleteController.class);

  public TransformationManager transformationManager;

  @Autowired
  public DeleteController(TransformationManager transformManager) {
    this.transformationManager = transformManager;
  }

  /**
   * Handles application/json DELETE transform requests to the /transform context. This method
   * handles: - Deleting the given transformId. - If the transformId is not there, a
   * TransformationException gets thrown. Returns an HTTP Status Code of 204 No Content on success;
   * otherwise, an error status code.
   *
   * @param transformId - the unique ID of the transform resource
   * @throws TransformationException if an error occurs while executing this method
   * @throws TransformationNotFoundException if the transformId is not there
   * @return the response of the request with the HTTP status code and the Content-Version
   */
  @Override
  public ResponseEntity<Void> delete(
      @ApiParam(value = "The ID of the transform request. ", required = true)
          @PathVariable("TransformId")
          String transformId)
      throws TransformationException {
    transformationManager.delete(transformId);
    return ResponseEntity.noContent().build();
  }
}
