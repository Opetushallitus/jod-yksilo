/*
 * Copyright (c) 2024 Finnish Ministry of Education and Culture,
 * Finnish Ministry of Economic Affairs and Employment.
 * Licensed under the EUPL-1.2-or-later.
 *
 * This file is part of jod-yksilo.
 */

package fi.okm.jod.yksilo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Spring Security filter chain configuration. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.securityMatcher("/api/**")
        .authorizeHttpRequests(
            authorize -> {
              authorize.requestMatchers("/api/v1/ping").permitAll();
              authorize.anyRequest().authenticated();
            })
        .build();
  }
}
