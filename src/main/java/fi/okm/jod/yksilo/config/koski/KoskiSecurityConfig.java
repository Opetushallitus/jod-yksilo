/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.koski;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;

@Slf4j
@ConditionalOnBean(KoskiOAuth2Config.class)
@Configuration(KoskiSecurityConfig.SECURITY_CONFIG)
public class KoskiSecurityConfig {

  public static final String SECURITY_CONFIG = "koski-security-config";

  private static final String AUTHORIZATION_URL = "/oauth2/authorization/koski";
  private static final String AUTHORIZE_CALLBACK_URL = "/oauth2/response/koski";
  private static final String AUTHORIZE_URL = "/oauth2/authorize/koski";

  @Bean
  public SecurityFilterChain oauthConfig(
      HttpSecurity http,
      ClientRegistrationRepository repository,
      @Qualifier(KoskiRestClientConfig.RESTCLIENT_ID) RestClient oauthRestClient,
      HttpSessionOAuth2AuthorizedClientRepository authorizedClientRepository)
      throws Exception {
    log.info("Configuring Koski OAuth2 integration...");

    return http.csrf(
            csrf -> csrf.ignoringRequestMatchers(AUTHORIZATION_URL, AUTHORIZE_CALLBACK_URL))
        .securityMatcher(AUTHORIZATION_URL, AUTHORIZE_CALLBACK_URL, AUTHORIZE_URL)
        .authorizeHttpRequests(
            auth ->
                auth
                    // Require authentication for these endpoints
                    .requestMatchers(AUTHORIZE_URL, AUTHORIZATION_URL)
                    .authenticated()
                    // Allow open access to these endpoints
                    .requestMatchers(AUTHORIZE_CALLBACK_URL)
                    .permitAll()
                    // In case any other request comes through this chain, permit it.
                    .anyRequest()
                    .permitAll())
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
}
