/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.koski;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@ConditionalOnBean(KoskiOAuth2Config.class)
@Configuration
public class KoskiSecurityConfig {

  private static final String AUTHORIZE_CALLBACK_URL = "/oauth2/response/koski";

  @Bean
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain oauthConfig(
      HttpSecurity http,
      ClientRegistrationRepository repository,
      @Qualifier(KoskiRestClientConfig.OAUTH2_RESTCLIENT_ID) RestClient oauthRestClient,
      HttpSessionOAuth2AuthorizedClientRepository authorizedClientRepository)
      throws Exception {
    log.info("Configuring Koski OAuth2 integration...");

    return http.securityMatcher("/oauth2/*/koski")
        .csrf(csrf -> csrf.ignoringRequestMatchers(AUTHORIZE_CALLBACK_URL))
        .requestCache(RequestCacheConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2Client(
            client -> {
              client.authorizationCodeGrant(
                  code -> {
                    code.authorizationRequestResolver(
                        createKoskiAuthorizationRequestResolver(repository));
                    code.accessTokenResponseClient(
                        createAccessTokenResponseClient(oauthRestClient));
                  });
              client.authorizedClientRepository(authorizedClientRepository);
            })
        .addFilterBefore(
            new DenyUnauthenticatedFilter(), OAuth2AuthorizationRequestRedirectFilter.class)
        .build();
  }

  private static RestClientAuthorizationCodeTokenResponseClient createAccessTokenResponseClient(
      RestClient oauthRestClient) {
    var responseClient = new RestClientAuthorizationCodeTokenResponseClient();
    responseClient.setRestClient(oauthRestClient);
    return responseClient;
  }

  private static DefaultOAuth2AuthorizationRequestResolver createKoskiAuthorizationRequestResolver(
      ClientRegistrationRepository repository) {
    var resolver =
        new DefaultOAuth2AuthorizationRequestResolver(
            repository,
            OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
    resolver.setAuthorizationRequestCustomizer(
        OAuth2AuthorizationRequestCustomizers.withPkce()
            .andThen(
                customizer ->
                    customizer.additionalParameters(
                        additionalParameters ->
                            additionalParameters.put("response_mode", "form_post"))));
    return resolver;
  }

  @Bean
  public HttpSessionOAuth2AuthorizedClientRepository authorizedClientRepository() {
    return new HttpSessionOAuth2AuthorizedClientRepository();
  }

  static class DenyUnauthenticatedFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      var securityContext = SecurityContextHolder.getContext();
      if (securityContext.getAuthentication() == null
          || !securityContext.getAuthentication().isAuthenticated()) {
        throw new InsufficientAuthenticationException("Authentication required");
      }
      filterChain.doFilter(request, response);
    }
  }
}
