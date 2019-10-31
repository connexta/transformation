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

import com.connexta.transformation.commons.api.MetadataTransformation;
import com.connexta.transformation.commons.api.TransformationManager;
import com.connexta.transformation.commons.api.exceptions.TransformationException;
import com.connexta.transformation.commons.api.exceptions.TransformationNotFoundException;
import com.connexta.transformation.rest.models.MetadataType;
import com.connexta.transformation.rest.spring.TransformApiRetrieve;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransformRetrieveController implements TransformApiRetrieve {

  private final TransformationManager transformationManager;

  public TransformRetrieveController(TransformationManager transformationManager) {
    this.transformationManager = transformationManager;
  }

  @Override
  public ResponseEntity<Resource> retrieve(
      @ApiParam(value = "The ID of the transform request. ", required = true)
          @PathVariable("TransformId")
          String transformId,
      @ApiParam(value = "The metadata format ID.", required = true, allowableValues = "\"irm\"")
          @PathVariable("MetadataType")
          MetadataType metadataType)
      throws TransformationException, IOException {
    MetadataTransformation transformation =
        transformationManager.get(transformId, metadataType.name());
    Resource resource =
        new InputStreamResource(
            transformation
                .getContent()
                .orElseThrow(
                    () ->
                        new TransformationNotFoundException(
                            "Successful transformation had no metadata.")));

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_TYPE,
            transformation.getContentType().orElse("application/octet-stream"))
        .header(
            HttpHeaders.CONTENT_LENGTH,
            String.valueOf(transformation.getContentLength().orElse(-1L)))
        .body(resource);
  }
}
