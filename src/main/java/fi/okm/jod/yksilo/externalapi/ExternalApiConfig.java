/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.externalapi;

import fi.okm.jod.yksilo.config.JodRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class ExternalApiConfig {

  @Value("${jod.external-api.apiKey}")
  private String apiKey;

  @Bean
  @Order(2)
  public SecurityFilterChain externalApiFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/external-api/**")
        .addFilterBefore(
            new ExtApiKeyFilter(this.apiKey), UsernamePasswordAuthenticationFilter.class)
        .csrf(csrf -> csrf.disable())
        .requestCache(rc -> rc.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority(JodRole.EXTERNAL_API.name()));
    return http.build();
  }
}
