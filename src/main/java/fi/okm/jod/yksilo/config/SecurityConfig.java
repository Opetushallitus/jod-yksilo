/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.CacheControlConfig;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Spring security filter chain configuration. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

  @Bean
  @SuppressWarnings({"java:S4502", "java:S5411"})
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      Environment env,
      @Value("${jod.session.maxDuration}") Duration sessionMaxDuration)
      throws Exception {

    RequestMatcher[] csrfIgnoringRequestMatchers;
    RequestMatcher notAuthenticated =
        request ->
            request.getSession(false) == null
                || SecurityContextHolder.getContext().getAuthentication() == null;

    if (env.matchesProfiles("local")
        && env.getProperty("springdoc.swagger-ui.enabled", Boolean.class, false)) {
      csrfIgnoringRequestMatchers =
          new RequestMatcher[] {
            notAuthenticated,
            request -> {
              try {
                if (InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()) {
                  log.warn("Allowing request without CSRF");
                  return true;
                }
              } catch (UnknownHostException e) {
                throw new UncheckedIOException(e);
              }
              return false;
            }
          };
    } else {
      csrfIgnoringRequestMatchers = new RequestMatcher[] {notAuthenticated};
    }

    return http.securityMatcher("/api/**")
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .requestCache(RequestCacheConfigurer::disable)
        .csrf(
            csrf -> {
              csrf.ignoringRequestMatchers(csrfIgnoringRequestMatchers);
              csrf.csrfTokenRequestHandler(new HardenedCsrfTokenRequestHandler());
            })
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(
                        "/api/ehdotus/**",
                        "/api/osaamiset",
                        "/api/ammatit",
                        "/api/tyomahdollisuudet/**",
                        "/api/koulutusmahdollisuudet/**",
                        "/api/keskustelut/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptionHandling -> {
              exceptionHandling.authenticationEntryPoint(
                  (request, response, authException) -> {
                    request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, authException);
                    request.getRequestDispatcher("/error").forward(request, response);
                  });
              exceptionHandling.accessDeniedPage("/error");
            })
        .headers(
            headers -> {
              // see https://infosec.mozilla.org/guidelines/web_security#content-security-policy
              headers.contentSecurityPolicy(
                  csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"));

              headers.cacheControl(CacheControlConfig::disable);
              headers.addHeaderWriter(new CacheControlHeadersWriter());
            })
        .addFilterBefore(
            (request, response, chain) -> {
              if (request instanceof HttpServletRequest req
                  && req.getSession(false) instanceof HttpSession session
                  && (System.currentTimeMillis() - session.getCreationTime())
                      > sessionMaxDuration.toMillis()) {
                req.logout();
                throw new AccessDeniedException("Session maximum duration exceeded");
              }
              chain.doFilter(request, response);
            },
            AuthorizationFilter.class)
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

  static final class HardenedCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
    private final XorCsrfTokenRequestAttributeHandler delegate =
        new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(
        HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
      delegate.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
      try {
        // fails with IllegalArgumentException if the token is invalid instead of returning null
        return delegate.resolveCsrfTokenValue(request, csrfToken);
      } catch (Exception e) {
        log.warn("Failed to resolve CSRF token value: {}", e.toString());
        return null;
      }
    }
  }
}
