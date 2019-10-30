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

import com.connexta.transformation.commons.api.ErrorCode;
import com.connexta.transformation.commons.api.MetadataTransformation;
import com.connexta.transformation.commons.api.Transformation;
import com.connexta.transformation.commons.api.TransformationStatus.State;
import com.connexta.transformation.rest.models.ErrorMessage;
import com.connexta.transformation.rest.models.MetadataInformation;
import com.connexta.transformation.rest.models.MetadataType;
import com.connexta.transformation.rest.models.Status;
import com.connexta.transformation.rest.models.TransformationPollResponse;
import com.connexta.transformation.service.Exceptions.UnknownStateException;
import java.net.URI;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * PollResponseCreator is a method object that is used to create a {@link
 * TransformationPollResponse} with the information provided by a {@link Transformation}.
 */
class PollResponseCreator {

  private static final Map<State, Status> statusMapping =
      Map.of(
          State.FAILED,
          Status.FAILED,
          State.IN_PROGRESS,
          Status.IN_PROGRESS,
          State.SUCCESSFUL,
          Status.DONE);

  private final Transformation transform;

  private final URI baseUri;

  private PollResponseCreator(Transformation transform, URI baseUri) {
    this.transform = transform;
    this.baseUri = baseUri;
  }

  /**
   * Creates a {@link TransformationPollResponse} using the information provided in the given
   * transformation.
   *
   * @param transform the transformation to produce a poll response from
   * @param baseUri the base URI which will be used for creating metadata location URIs
   * @throws UnknownStateException if the transformation is in an unknown state
   * @throws IllegalArgumentException the baseUri has an invalid syntax
   * @throws IllegalStateException if the transformation is missing fields required for its current
   *     state.
   * @return a {@link TransformationPollResponse} with the provided information
   */
  static TransformationPollResponse convertToPollResponse(Transformation transform, URI baseUri)
      throws UnknownStateException {
    return new PollResponseCreator(transform, baseUri).convertToPollResponse();
  }

  private TransformationPollResponse convertToPollResponse() throws UnknownStateException {
    Status status = statusMapping.get(transform.getState());
    if (status == null) {
      throw new UnknownStateException(
          "Could not create polling response because the transformation was in an unknown state.");
    }

    TransformationPollResponse pollResponse =
        new TransformationPollResponse()
            .transformationStatus(status)
            .metadataInformations(extractMetadataInformations());

    if (transform.hasFailed()) {
      pollResponse.setErrorMessage(extractErrorInfo());
    }

    return pollResponse;
  }

  private List<MetadataInformation> extractMetadataInformations() {
    return transform
        .metadatas()
        .map(this::convertToMetadataInformation)
        .collect(Collectors.toList());
  }

  private MetadataInformation convertToMetadataInformation(
      MetadataTransformation metadataTransform) {
    MetadataInformation info = new MetadataInformation();
    info.metadataType(MetadataType.fromValue(metadataTransform.getMetadataType()))
        .transformationStatus(statusMapping.get(metadataTransform.getState()))
        .errorMessage(extractErrorMessage(metadataTransform));

    if (metadataTransform.isCompleted()) {
      info.transformedTimestamp(
          getOrThrowIllegalState(
                  metadataTransform.getCompletionTime(),
                  "Completed metadata transformation contained no completion time")
              .atOffset(ZoneOffset.UTC));
    }

    if (metadataTransform.wasSuccessful()) {
      info.location(URI.create(baseUri.toString() + "/" + metadataTransform.getMetadataType()));
    }

    return info;
  }

  @Nullable
  private ErrorMessage extractErrorMessage(MetadataTransformation metadataTransform) {
    if (metadataTransform.hasFailed()) {
      ErrorMessage errorMessage = new ErrorMessage();
      errorMessage.message(
          getOrThrowIllegalState(
              metadataTransform.getFailureMessage(),
              "Failed metadata transformation contained no failure message."));
      errorMessage.addDetailsItem(
          getOrThrowIllegalState(
                  metadataTransform.getFailureReason(),
                  "Failed metadata transformation contained no failure reason.")
              .name());
      return errorMessage;
    } else {
      return null;
    }
  }

  private ErrorMessage extractErrorInfo() {
    ErrorMessage errorMessage = new ErrorMessage();
    errorMessage.message(
        "Transformation failed. Check details for error codes, or, check metadataInformations for specific error messages.");
    errorMessage.details(collectTransformationFailureReasons());

    return errorMessage;
  }

  private List<String> collectTransformationFailureReasons() {
    return transform
        .metadatas()
        .filter(MetadataTransformation::hasFailed)
        .map(MetadataTransformation::getFailureReason)
        .map(
            reason ->
                getOrThrowIllegalState(
                    reason, "Failed metadata transformation contained no failure message."))
        .map(ErrorCode::name)
        .collect(Collectors.toList());
  }

  private <T> T getOrThrowIllegalState(Optional<T> optional, String exceptionMessage) {
    return optional.orElseThrow(() -> new IllegalStateException(exceptionMessage));
  }
}
