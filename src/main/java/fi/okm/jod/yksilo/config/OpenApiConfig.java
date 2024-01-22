/*
 * Copyright (c) 2024 Finnish Ministry of Education and Culture,
 * Finnish Ministry of Economic Affairs and Employment.
 * Licensed under the EUPL-1.2-or-later.
 *
 * This file is part of jod-yksilo.
 */

package fi.okm.jod.yksilo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** OpenAPI description configuration. */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
class OpenApiConfig {

  /** Creates OpenAPI documentation bean. */
  @Bean
  public OpenAPI openApi() {
    var pkg = getClass().getPackage();
    return new OpenAPI()
        .info(
            new Info().title(pkg.getImplementationTitle()).version(pkg.getImplementationVersion()));
  }

  @Bean
  @Order(1)
  public SecurityFilterChain openApiFilterChain(HttpSecurity http) throws Exception {
    return http.securityMatcher("/openapi/**")
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .build();
  }
}
