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
import com.connexta.transformation.commons.inmemory.InMemoryTransformationManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Main class for the Transformation implementation application. This class also is the
 * configuration class for the application.
 */
@SpringBootApplication
public class Application {

  private static final int MAX_PAYLOAD_LENGTH = 5000;

  private static final String INBOUND_REQUEST_PREFIX = "Inbound Request: ";

  @Value("${endpoints.lookupService.url}")
  private String lookupServiceUrl;

  @Bean
  public CommonsRequestLoggingFilter requestLoggingFilter() {
    final CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
    loggingFilter.setIncludeClientInfo(true);
    loggingFilter.setIncludeQueryString(true);
    loggingFilter.setIncludePayload(true);
    loggingFilter.setIncludeHeaders(true);
    loggingFilter.setAfterMessagePrefix(INBOUND_REQUEST_PREFIX);
    loggingFilter.setMaxPayloadLength(MAX_PAYLOAD_LENGTH);
    return loggingFilter;
  }

  @Bean(name = "lookupServiceUrl")
  public String getLookupServiceUrl() {
    return lookupServiceUrl;
  }

  @Bean
  public TransformationManager transformationManager() {
    return new InMemoryTransformationManager();
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
