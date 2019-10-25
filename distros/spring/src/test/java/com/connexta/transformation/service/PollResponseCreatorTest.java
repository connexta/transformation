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
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class PollResponseCreatorTest {
  private static final String ID = "id123";

  private static final String URI = "/transform/" + ID;

  private static final String IRM = "irm";

  @Test
  public void convertTransformationWithNoMetadataToPollResponse() throws Exception {
    Transformation mockTransform = mockTransformation();

    TransformationPollResponse expected =
        new TransformationPollResponse()
            .transformationStatus(Status.IN_PROGRESS)
            .metadataInformations(List.of());

    assertEquals(expected, convertToPollResponse(mockTransform, new URI(URI)));
  }

  @Test
  public void convertTransformationWithInProgressMetadataToPollResponse() throws Exception {
    MetadataTransformation mockMetadata = mockMetadataTransformation(State.IN_PROGRESS);

    Transformation mockTransform = mockTransformation(mockMetadata);

    MetadataInformation expectedMetadata =
        new MetadataInformation()
            .metadataType(MetadataType.IRM)
            .transformationStatus(Status.IN_PROGRESS);

    TransformationPollResponse expected =
        new TransformationPollResponse()
            .transformationStatus(Status.IN_PROGRESS)
            .metadataInformations(List.of(expectedMetadata));

    assertEquals(expected, convertToPollResponse(mockTransform, new URI(URI)));
  }

  @Test
  public void convertTransformationWithSuccessfulMetadataToPollResponse() throws Exception {
    MetadataTransformation mockMetadata = mockMetadataTransformation(State.SUCCESSFUL);
    when(mockMetadata.getCompletionTime()).thenReturn(Optional.of(Instant.EPOCH));

    Transformation mockTransform = mockTransformation(mockMetadata);

    URI location = new URI(String.format("/transform/%s/%s", ID, IRM));
    MetadataInformation expectedMetadata =
        new MetadataInformation()
            .metadataType(MetadataType.IRM)
            .transformationStatus(Status.DONE)
            .location(location)
            .transformedTimestamp(Instant.EPOCH.atOffset(ZoneOffset.UTC));

    TransformationPollResponse expected =
        new TransformationPollResponse()
            .transformationStatus(Status.DONE)
            .metadataInformations(List.of(expectedMetadata));

    assertEquals(expected, convertToPollResponse(mockTransform, new URI(URI)));
  }

  @Test
  public void convertTransformationWithFailedMetadataToPollResponse() throws Exception {
    String failureMessage = "The thing did bad";

    MetadataTransformation mockMetadata = mockMetadataTransformation(State.FAILED);
    when(mockMetadata.getFailureMessage()).thenReturn(Optional.of(failureMessage));
    when(mockMetadata.getFailureReason()).thenReturn(Optional.of(ErrorCode.TRANSFORMATION_FAILURE));
    when(mockMetadata.getCompletionTime()).thenReturn(Optional.of(Instant.EPOCH));

    Transformation mockTransform = mockTransformation(mockMetadata);

    ErrorMessage metadataErrorMessage =
        new ErrorMessage()
            .message(failureMessage)
            .addDetailsItem(ErrorCode.TRANSFORMATION_FAILURE.name());

    MetadataInformation expectedMetadata =
        new MetadataInformation()
            .metadataType(MetadataType.IRM)
            .transformationStatus(Status.FAILED)
            .transformedTimestamp(Instant.EPOCH.atOffset(ZoneOffset.UTC))
            .errorMessage(metadataErrorMessage);

    ErrorMessage transformationErrorMessage =
        new ErrorMessage()
            .message(
                "Transformation failed. Check details for error codes, or, check metadataInformations for specific error messages.")
            .details(expectedMetadata.getErrorMessage().getDetails());

    TransformationPollResponse expected =
        new TransformationPollResponse()
            .transformationStatus(Status.FAILED)
            .errorMessage(transformationErrorMessage)
            .metadataInformations(List.of(expectedMetadata));

    assertEquals(expected, convertToPollResponse(mockTransform, new URI(URI)));
  }

  @Test(expected = UnknownStateException.class)
  public void convertTransformationWithUnknownStateMetadataToPollResponse() throws Exception {
    MetadataTransformation mockMetadata = mockMetadataTransformation(State.UNKNOWN);

    Transformation mockTransform = mockTransformation(mockMetadata);

    convertToPollResponse(mockTransform, new URI(URI));
  }

  @Test
  public void convertTransformationWithMultipleMetadataToPollResponse() throws Exception {
    MetadataTransformation successfulMetadata = mockMetadataTransformation(State.SUCCESSFUL);
    when(successfulMetadata.getCompletionTime()).thenReturn(Optional.of(Instant.EPOCH));

    MetadataTransformation inProgressMetadata = mockMetadataTransformation(State.IN_PROGRESS);

    String failureMessage = "The thing did bad";
    MetadataTransformation failedMetadata = mockMetadataTransformation(State.FAILED);
    when(failedMetadata.getFailureMessage()).thenReturn(Optional.of(failureMessage));
    when(failedMetadata.getFailureReason())
        .thenReturn(Optional.of(ErrorCode.TRANSFORMATION_FAILURE));
    when(failedMetadata.getCompletionTime()).thenReturn(Optional.of(Instant.EPOCH));

    Transformation mockTransform =
        mockTransformation(successfulMetadata, inProgressMetadata, failedMetadata);

    URI location = new URI(String.format("/transform/%s/%s", ID, IRM));
    MetadataInformation expectedSuccessfulMetadata =
        new MetadataInformation()
            .metadataType(MetadataType.IRM)
            .transformationStatus(Status.DONE)
            .location(location)
            .transformedTimestamp(Instant.EPOCH.atOffset(ZoneOffset.UTC));

    MetadataInformation expectedInProgressMetadata =
        new MetadataInformation()
            .metadataType(MetadataType.IRM)
            .transformationStatus(Status.IN_PROGRESS);

    ErrorMessage metadataErrorMessage =
        new ErrorMessage()
            .message(failureMessage)
            .addDetailsItem(ErrorCode.TRANSFORMATION_FAILURE.name());

    MetadataInformation expectedFailedMetadata =
        new MetadataInformation()
            .metadataType(MetadataType.IRM)
            .transformationStatus(Status.FAILED)
            .transformedTimestamp(Instant.EPOCH.atOffset(ZoneOffset.UTC))
            .errorMessage(metadataErrorMessage);

    TransformationPollResponse expected =
        new TransformationPollResponse()
            .transformationStatus(Status.IN_PROGRESS)
            .metadataInformations(
                List.of(
                    expectedSuccessfulMetadata,
                    expectedInProgressMetadata,
                    expectedFailedMetadata));

    assertEquals(expected, convertToPollResponse(mockTransform, new URI(URI)));
  }

  @Test
  public void convertTransformationWithMultipleFailedMetadataToPollResponse() throws Exception {
    String failureMessage = "The thing did bad";

    MetadataTransformation mockMetadata = mockMetadataTransformation(State.FAILED);
    when(mockMetadata.getFailureMessage()).thenReturn(Optional.of(failureMessage));
    when(mockMetadata.getFailureReason()).thenReturn(Optional.of(ErrorCode.TRANSFORMATION_FAILURE));
    when(mockMetadata.getCompletionTime()).thenReturn(Optional.of(Instant.EPOCH));

    MetadataTransformation mockMetadata2 = mockMetadataTransformation(State.FAILED);
    when(mockMetadata2.getFailureMessage()).thenReturn(Optional.of(failureMessage));
    when(mockMetadata2.getFailureReason()).thenReturn(Optional.of(ErrorCode.UNKNOWN));
    when(mockMetadata2.getCompletionTime()).thenReturn(Optional.of(Instant.ofEpochMilli(1)));

    MetadataTransformation mockMetadata3 = mockMetadataTransformation(State.FAILED);
    when(mockMetadata3.getFailureMessage()).thenReturn(Optional.of(failureMessage));
    when(mockMetadata3.getFailureReason())
        .thenReturn(Optional.of(ErrorCode.TRANSFORMATION_FAILURE));
    when(mockMetadata3.getCompletionTime()).thenReturn(Optional.of(Instant.ofEpochMilli(2)));

    Transformation mockTransform = mockTransformation(mockMetadata, mockMetadata2, mockMetadata3);

    ErrorMessage metadataErrorMessage =
        new ErrorMessage()
            .message(failureMessage)
            .addDetailsItem(ErrorCode.TRANSFORMATION_FAILURE.name());

    ErrorMessage metadataErrorMessage2 =
        new ErrorMessage().message(failureMessage).addDetailsItem(ErrorCode.UNKNOWN.name());

    MetadataInformation expectedMetadata =
        new MetadataInformation()
            .metadataType(MetadataType.IRM)
            .transformationStatus(Status.FAILED)
            .transformedTimestamp(Instant.EPOCH.atOffset(ZoneOffset.UTC))
            .errorMessage(metadataErrorMessage);

    MetadataInformation expectedMetadata2 =
        new MetadataInformation()
            .metadataType(MetadataType.IRM)
            .transformationStatus(Status.FAILED)
            .transformedTimestamp(Instant.ofEpochMilli(1).atOffset(ZoneOffset.UTC))
            .errorMessage(metadataErrorMessage2);

    MetadataInformation expectedMetadata3 =
        new MetadataInformation()
            .metadataType(MetadataType.IRM)
            .transformationStatus(Status.FAILED)
            .transformedTimestamp(Instant.ofEpochMilli(2).atOffset((ZoneOffset.UTC)))
            .errorMessage(metadataErrorMessage);

    ErrorMessage transformationErrorMessage =
        new ErrorMessage()
            .message(
                "Transformation failed. Check details for error codes, or, check metadataInformations for specific error messages.");

    transformationErrorMessage.addDetailsItem(metadataErrorMessage.getDetails().get(0));
    transformationErrorMessage.addDetailsItem(metadataErrorMessage2.getDetails().get(0));
    transformationErrorMessage.addDetailsItem(metadataErrorMessage.getDetails().get(0));

    TransformationPollResponse expected =
        new TransformationPollResponse()
            .transformationStatus(Status.FAILED)
            .errorMessage(transformationErrorMessage)
            .metadataInformations(List.of(expectedMetadata, expectedMetadata2, expectedMetadata3));

    assertEquals(expected, convertToPollResponse(mockTransform, new URI(URI)));
  }

  @Test
  public void convertTransformationWithMultipleSuccessfulMetadataToPollResponse() throws Exception {
    MetadataTransformation mockMetadata1 = mockMetadataTransformation(State.SUCCESSFUL);
    when(mockMetadata1.getCompletionTime()).thenReturn(Optional.of(Instant.EPOCH));
    MetadataTransformation mockMetadata2 = mockMetadataTransformation(State.SUCCESSFUL);
    when(mockMetadata2.getCompletionTime()).thenReturn(Optional.of(Instant.ofEpochMilli(1)));
    MetadataTransformation mockMetadata3 = mockMetadataTransformation(State.SUCCESSFUL);
    when(mockMetadata3.getCompletionTime()).thenReturn(Optional.of(Instant.ofEpochMilli(2)));

    Transformation mockTransform = mockTransformation(mockMetadata1, mockMetadata2, mockMetadata3);

    URI location = new URI(String.format("/transform/%s/%s", ID, IRM));
    MetadataInformation expectedMetadata =
        new MetadataInformation()
            .metadataType(MetadataType.IRM)
            .transformationStatus(Status.DONE)
            .location(location)
            .transformedTimestamp(Instant.EPOCH.atOffset(ZoneOffset.UTC));
    MetadataInformation expectedMetadata2 =
        new MetadataInformation()
            .metadataType(MetadataType.IRM)
            .transformationStatus(Status.DONE)
            .location(location)
            .transformedTimestamp(Instant.ofEpochMilli(1).atOffset(ZoneOffset.UTC));
    MetadataInformation expectedMetadata3 =
        new MetadataInformation()
            .metadataType(MetadataType.IRM)
            .transformationStatus(Status.DONE)
            .location(location)
            .transformedTimestamp(Instant.ofEpochMilli(2).atOffset(ZoneOffset.UTC));

    TransformationPollResponse expected =
        new TransformationPollResponse()
            .transformationStatus(Status.DONE)
            .metadataInformations(List.of(expectedMetadata, expectedMetadata2, expectedMetadata3));

    assertEquals(expected, convertToPollResponse(mockTransform, new URI(URI)));
  }

  // These abstract classes let us use Mockito.CALLS_REAL_METHODS on our mocks so we can use the
  // default methods on their interfaces
  private abstract static class TestMetadataTransformation implements MetadataTransformation {}

  private abstract static class TestTransformation implements Transformation {}

  private Transformation mockTransformation(MetadataTransformation... metadataTransformations) {
    Transformation mockTransform =
        mock(Transformation.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
    when(mockTransform.getTransformId()).thenReturn(ID);
    when(mockTransform.metadatas())
        .thenAnswer((Answer) invocation -> Stream.of(metadataTransformations));
    return mockTransform;
  }

  private MetadataTransformation mockMetadataTransformation(State state) {
    MetadataTransformation mockMetadata =
        mock(
            MetadataTransformation.class, withSettings().defaultAnswer(Mockito.CALLS_REAL_METHODS));
    when(mockMetadata.getTransformId()).thenReturn(ID);
    when(mockMetadata.getMetadataType()).thenReturn(IRM);
    when(mockMetadata.getState()).thenReturn(state);
    return mockMetadata;
  }
}
