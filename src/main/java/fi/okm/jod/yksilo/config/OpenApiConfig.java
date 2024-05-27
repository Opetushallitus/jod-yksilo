/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.info.Info;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** OpenAPI description configuration. */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
@Slf4j
class OpenApiConfig {

  /** Creates OpenAPI documentation bean. */
  @Bean
  public OpenApiCustomizer openApi() {
    var pkg = getClass().getPackage();
    return openApi ->
        openApi.info(
            new Info().title(pkg.getImplementationTitle()).version(pkg.getImplementationVersion()));
  }

  /**
   * Customizes operation IDs to include the controller name and method name, e.g.
   * ToimenkuvaController.get becomes toimenkuvaGet instead of just "get"
   */
  @Bean
  public OperationCustomizer operationCustomizer() {
    return (operation, handlerMethod) -> {
      if (!handlerMethod.getMethod().isAnnotationPresent(Operation.class)) {
        operation.setOperationId(
            uncapitalize(handlerMethod.getBeanType().getSimpleName().replace("Controller", ""))
                + capitalize(handlerMethod.getMethod().getName()));
      }
      return operation;
    };
  }

  private static String capitalize(String str) {
    if (str == null || str.isEmpty()) return str;
    var first = str.substring(0, 1).toUpperCase(Locale.ROOT);
    return str.length() == 1 ? first : first + str.substring(1);
  }

  private static String uncapitalize(String str) {
    if (str == null || str.isEmpty()) return str;
    var first = str.substring(0, 1).toLowerCase(Locale.ROOT);
    return str.length() == 1 ? first : first + str.substring(1);
  }

  @Bean
  @Order(1)
  public SecurityFilterChain openApiFilterChain(HttpSecurity http) throws Exception {
    return http.securityMatcher("/openapi/**")
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.NEVER))
        .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
        .build();
  }
}
