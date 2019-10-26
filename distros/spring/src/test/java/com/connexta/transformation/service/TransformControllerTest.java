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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connexta.transformation.commons.api.TransformationManager;
import com.connexta.transformation.commons.api.exceptions.TransformationNotFoundException;
import com.connexta.transformation.rest.models.TransformRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

@RunWith(SpringRunner.class)
@WebMvcTest(TransformController.class)
public class TransformControllerTest {

  private static final String TEST_URI = "http://test:9000";

  private static final String ACCEPT_VERSION = "Accept-Version";

  private static final String ACCEPT_VERSION_NUM = "0.1.0";

  private ObjectMapper mapper = new ObjectMapper();

  @Autowired private MockMvc mockMvc;

  @MockBean TransformationManager tManager;

  @MockBean(answer = Answers.RETURNS_MOCKS)
  private RestTemplateBuilder restTemplateBuilder;

  @Test
  public void testAcceptedResponseEntity() throws Exception {
    this.mockMvc
        .perform(
            post("/transform")
                .header(ACCEPT_VERSION, ACCEPT_VERSION_NUM)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsString(
                        new TransformRequest()
                            .currentLocation(new URI(TEST_URI))
                            .finalLocation(new URI(TEST_URI))
                            .metacardLocation(new URI(TEST_URI))))
                .characterEncoding(StandardCharsets.UTF_8.name()))
        .andExpect(status().isAccepted());
  }

  @Test(expected = ResourceAccessException.class)
  public void testInvalidServiceUrl() {
    String badServiceUrl = "http://bad.url";
    TransformController controller =
        new TransformController(new RestTemplateBuilder(), badServiceUrl, tManager);

    controller.transform(ACCEPT_VERSION, new TransformRequest());
  }

  @Test
  public void testDelete() throws Exception {
    this.mockMvc.perform(delete("/transform/id123")).andExpect(status().isNoContent());
    verify(tManager).delete(anyString());
  }

  @Test
  public void testNotFoundDelete() throws Exception {
    doThrow(new TransformationNotFoundException("Transformation [gibberish-ID] cannot be found"))
        .when(tManager)
        .delete(anyString());
    assertThatThrownBy(() -> this.mockMvc.perform(delete("/transform/gibberish-ID")))
        .hasCause(
            new TransformationNotFoundException("Transformation [gibberish-ID] cannot be found"));
  }
}
