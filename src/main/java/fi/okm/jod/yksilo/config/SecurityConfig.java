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
import java.time.Duration;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.CacheControlConfig;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.util.StringUtils;

/** Spring security filter chain configuration. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

  @Bean
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, @Value("${jod.session.maxDuration}") Duration sessionMaxDuration)
      throws Exception {
    return http.securityMatcher("/api/**")
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .requestCache(RequestCacheConfigurer::disable)
        .csrf(
            csrf -> {
              csrf.ignoringRequestMatchers(request -> request.getSession(false) == null);
              csrf.csrfTokenRequestHandler(new HardenedCsrfTokenRequestHandler());
            })
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/api/ehdotus/**")
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

  /** Mock authentication using default form login. */
  @Bean
  SecurityFilterChain loginFilterChain(HttpSecurity http) throws Exception {
    log.warn("WARNING: Using mock authentication.");

    var redirectStrategy = new DefaultRedirectStrategy();
    redirectStrategy.setStatusCode(HttpStatus.SEE_OTHER);

    var loginSuccessHandler = new SimpleUrlAuthenticationSuccessHandler("/?loginSuccess");
    loginSuccessHandler.setRedirectStrategy(redirectStrategy);

    var logoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
    logoutSuccessHandler.setRedirectStrategy(redirectStrategy);

    return http.securityMatcher("/login", "/logout")
        .formLogin(login -> login.successHandler(loginSuccessHandler))
        .logout(logout -> logout.logoutSuccessHandler(logoutSuccessHandler))
        .headers(
            headers ->
                headers.contentSecurityPolicy(
                    csp ->
                        csp.policyDirectives(
                            "default-src 'self'; frame-ancestors 'none'; style-src 'self' https://maxcdn.bootstrapcdn.com https://getbootstrap.com;")))
        .build();
  }

  @Bean
  UserDetailsService mockUserDetailsService() {
    log.warn("WARNING: Using mock user details service.");
    return username -> {
      if (!StringUtils.hasLength(username) || username.length() > 100) {
        throw new UsernameNotFoundException("Invalid username");
      }
      return User.builder().username(username).password("{noop}password").roles("USER").build();
    };
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
