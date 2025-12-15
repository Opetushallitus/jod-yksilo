/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.tmt;

import fi.okm.jod.yksilo.config.DenyUnauthenticatedFilter;
import fi.okm.jod.yksilo.config.SessionLoginAttribute;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "jod.tmt.enabled", havingValue = "true")
public class TmtSecurityConfig {

  @Bean
  public SecurityFilterChain exportOauth2Config(
      HttpSecurity http,
      ClientRegistrationRepository repository,
      HttpSessionOAuth2AuthorizedClientRepository authorizedClientRepository,
      RestClient.Builder restClientBuilder,
      TmtConfiguration tmtConfiguration)
      throws Exception {
    log.info("Configuring TMT OAuth2 export integration...");

    return buildFilterChain(
        http,
        "tmt-vienti",
        repository,
        authorizedClientRepository,
        tmtExportOauth2RestClient(restClientBuilder, tmtConfiguration));
  }

  @Bean
  public SecurityFilterChain importOauth2Config(
      HttpSecurity http,
      ClientRegistrationRepository repository,
      HttpSessionOAuth2AuthorizedClientRepository authorizedClientRepository,
      RestClient.Builder builder,
      TmtConfiguration tmtConfiguration)
      throws Exception {
    log.info("Configuring TMT OAuth2 import integration...");

    return buildFilterChain(
        http,
        "tmt-haku",
        repository,
        authorizedClientRepository,
        tmtImportOauth2RestClient(builder, tmtConfiguration));
  }

  private static SecurityFilterChain buildFilterChain(
      HttpSecurity http,
      String registrationId,
      ClientRegistrationRepository registrationRepository,
      HttpSessionOAuth2AuthorizedClientRepository authorizedClientRepository,
      RestClient restClient)
      throws Exception {
    return http.securityMatcher("/oauth2/*/" + registrationId)
        .requestCache(RequestCacheConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2Client(
            client ->
                client
                    .clientRegistrationRepository(registrationRepository)
                    .authorizedClientRepository(authorizedClientRepository)
                    .authorizationCodeGrant(
                        grant ->
                            grant.accessTokenResponseClient(
                                createAccessTokenResponseClient(restClient))))
        .addFilterBefore(
            new CallbackFilter(registrationId), OAuth2AuthorizationRequestRedirectFilter.class)
        .addFilterBefore(new DenyUnauthenticatedFilter(), CallbackFilter.class)
        .build();
  }

  private static OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
      createAccessTokenResponseClient(RestClient oauthRestClient) {
    final var responseClient = new RestClientAuthorizationCodeTokenResponseClient();
    responseClient.setRestClient(oauthRestClient);

    return request -> {
      try {
        return responseClient.getTokenResponse(request);
      } catch (OAuth2AuthorizationException ex) {
        // the error_description can leak sensitive info,
        // so log it and convert to a generic one
        log.warn("TMT OAuth2 authorization failed: {}", ex.getError());
        throw new OAuth2AuthorizationException(new OAuth2Error(ex.getError().getErrorCode()), ex);
      }
    };
  }

  @RequiredArgsConstructor
  static class CallbackFilter extends OncePerRequestFilter {
    private final String registrationId;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws java.io.IOException, ServletException {
      if (request.getRequestURI() instanceof String uri
          && uri.startsWith(request.getContextPath() + "/oauth2/authorization/" + registrationId)
          && "GET".equals(request.getMethod())
          && request.getParameter("callback") instanceof String cb
          && request.getSession(false) instanceof HttpSession session) {
        session.setAttribute(
            SessionLoginAttribute.CALLBACK_FRONTEND.getKey(), URI.create(cb).getPath());
      }
      filterChain.doFilter(request, response);
    }
  }

  @Bean(autowireCandidate = false)
  @RefreshScope
  RestClient tmtExportOauth2RestClient(RestClient.Builder builder, TmtConfiguration configuration) {
    return createRestClient(builder, configuration.getExportApi().getKipaSubscriptionKey());
  }

  @Bean(autowireCandidate = false)
  @RefreshScope
  RestClient tmtImportOauth2RestClient(RestClient.Builder builder, TmtConfiguration configuration) {
    return createRestClient(builder, configuration.getImportApi().getKipaSubscriptionKey());
  }

  private static RestClient createRestClient(RestClient.Builder builder, String subscriptionKey) {
    var messageConverters =
        List.of(
            new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter());

    var requestFactory =
        ClientHttpRequestFactoryBuilder.jdk()
            .build(
                ClientHttpRequestFactorySettings.defaults()
                    .withConnectTimeout(Duration.ofSeconds(5))
                    .withReadTimeout(Duration.ofSeconds(10)));

    return builder
        .requestFactory(requestFactory)
        .defaultHeader("KIPA-Subscription-Key", subscriptionKey)
        .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
        .messageConverters(messageConverters)
        .build();
  }
}
