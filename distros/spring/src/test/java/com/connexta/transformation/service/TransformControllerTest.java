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

import static org.hamcrest.core.Is.is;

import com.connexta.transformation.rest.models.TransformRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResourceAccessException;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "endpoints.lookupService.url = http://localhost:")
@ContextConfiguration
public class TransformControllerTest {
  private static final String ACCEPT_VERSION = "0.1.0";

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private String lookupServiceUrl;

  @Test
  public void testAcceptedResponseEntity() {
    TransformController controller =
        new TransformController(restTemplate.getRestTemplate(), lookupServiceUrl + port);
    ResponseEntity responseEntity = controller.transform(ACCEPT_VERSION, new TransformRequest());

    Assert.assertThat(responseEntity.getStatusCode(), is(HttpStatus.ACCEPTED));
  }

  @Test(expected = ResourceAccessException.class)
  public void testInvalidServiceUrl() {
    String badServiceUrl = "http://bad.url";
    TransformController controller =
        new TransformController(restTemplate.getRestTemplate(), badServiceUrl);

    controller.transform(ACCEPT_VERSION, new TransformRequest());
  }
}
