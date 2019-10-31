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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connexta.transformation.commons.api.Transformation;
import com.connexta.transformation.commons.api.TransformationManager;
import com.connexta.transformation.commons.api.TransformationStatus.State;
import com.connexta.transformation.commons.api.exceptions.TransformationException;
import com.connexta.transformation.commons.api.exceptions.TransformationNotFoundException;
import com.connexta.transformation.rest.models.Status;
import com.connexta.transformation.rest.models.TransformationPollResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = TransformStatusController.class)
public class TransformStatusControllerTest {

  private static final String ID = "id123";

  private static final String TEST_PATH = "/transform/" + ID;

  @Rule public ExpectedException expectedException = ExpectedException.none();

  private ObjectMapper mapper =
      new ObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .registerModule(new JavaTimeModule())
          .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

  @Autowired private MockMvc mockMvc;

  @MockBean private TransformationManager transformationManager;

  @Test
  public void testGetTransformationStatus() throws Exception {
    Transformation mockTransform = mock(Transformation.class);
    when(mockTransform.getState()).thenReturn(State.IN_PROGRESS);
    when(mockTransform.metadatas()).thenReturn(Stream.empty());
    when(transformationManager.get(anyString())).thenReturn(mockTransform);

    TransformationPollResponse expected =
        new TransformationPollResponse()
            .transformationStatus(Status.IN_PROGRESS)
            .metadataInformations(List.of());
    this.mockMvc
        .perform(get(TEST_PATH))
        .andExpect(status().isOk())
        .andDo(print())
        .andExpect(content().json(mapper.writeValueAsString(expected)));
  }

  @Test
  public void testGetTransformationStatusThrowsResponseStatusException() throws Exception {
    Transformation mockTransform = mock(Transformation.class);
    when(mockTransform.getState()).thenReturn(State.UNKNOWN);
    when(mockTransform.metadatas()).thenReturn(Stream.empty());
    when(transformationManager.get(anyString())).thenReturn(mockTransform);

    this.mockMvc.perform(get(TEST_PATH)).andExpect(status().isServiceUnavailable());
  }

  @Test
  public void testGetTransformationStatusDoesNotCatchTransformationException() throws Exception {
    when(transformationManager.get(anyString())).thenThrow(new TransformationException(""));
    expectedException.expectCause(Matchers.isA(TransformationException.class));

    this.mockMvc.perform(get(TEST_PATH));
  }

  @Test
  public void testGetTransformationStatusDoesNotCatchTransformationNotFoundException()
      throws Exception {
    when(transformationManager.get(anyString())).thenThrow(new TransformationNotFoundException(""));
    expectedException.expectCause(Matchers.isA(TransformationNotFoundException.class));

    this.mockMvc.perform(get(TEST_PATH));
  }
}
