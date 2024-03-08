/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.CacheControlConfig;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.HeaderWriter;

/** Spring security filter chain configuration. */
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
        .headers(
            headers -> {
              // see https://infosec.mozilla.org/guidelines/web_security#content-security-policy
              headers.contentSecurityPolicy(
                  csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"));

              headers.cacheControl(CacheControlConfig::disable);
              headers.addHeaderWriter(new CacheControlHeadersWriter());
            })
        .build();
  }

  static final class CacheControlHeadersWriter implements HeaderWriter {
    /*
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Caching#dont_cache
     *
     * stale-if-error=0, see:
     * https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/Expiration.html#ExpirationDownloadDist
     * If the origin is [now] unreachable and the minimum or maximum TTL value is greater than 0,
     * CloudFront will serve the object that it got from the origin previously. To avoid this
     * behavior, include the Cache-Control: stale-if-error=0 directive.
     */
    private static final String CACHE_CONTROL_HEADER_VALUE =
        "private, no-cache, no-store, stale-if-error=0";

    @Override
    public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
      if (!response.containsHeader(HttpHeaders.CACHE_CONTROL)) {
        response.addHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_HEADER_VALUE);
      }
    }
  }
}
