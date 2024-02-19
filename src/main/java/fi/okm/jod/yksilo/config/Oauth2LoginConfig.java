/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class Oauth2LoginConfig {

  @Bean
  SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/oauth2/**", "/login/**");
    http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());
    http.oauth2Login(withDefaults());
    http.oauth2Client(withDefaults());
    return http.build();
  }
}
