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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.connexta.transformation.commons.api.TransformationManager;
import com.connexta.transformation.commons.api.exceptions.TransformationNotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(DeleteController.class)
public class DeleteControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean TransformationManager tManager;

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
