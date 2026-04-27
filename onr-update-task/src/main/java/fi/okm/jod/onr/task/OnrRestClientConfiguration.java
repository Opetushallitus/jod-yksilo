/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.onr.task;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.client.RestClient;

/** Configures the OAuth2-authenticated RestClient for the ONR API. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({OnrTaskProperties.class, BatchTaskProperties.class})
class OnrRestClientConfiguration {

  private static final String REGISTRATION_ID = "onr";

  @Bean
  RestClient onrRestClient(RestClient.Builder builder, OnrTaskProperties config) {
    var registration =
        ClientRegistration.withRegistrationId(REGISTRATION_ID)
            .clientId(config.oauth2().clientId())
            .clientSecret(config.oauth2().clientSecret())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .tokenUri(config.oauth2().tokenUri())
            .build();

    var registrations = new InMemoryClientRegistrationRepository(registration);
    var service = new InMemoryOAuth2AuthorizedClientService(registrations);
    var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, service);
    manager.setAuthorizedClientProvider(
        OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());

    return builder
        .baseUrl(config.baseUrl())
        .requestInterceptor(
            (request, body, execution) -> {
              var authorizeRequest =
                  OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                      .principal("onr-task")
                      .build();
              var client = manager.authorize(authorizeRequest);
              if (client != null) {
                request.getHeaders().setBearerAuth(client.getAccessToken().getTokenValue());
              }
              return execution.execute(request, body);
            })
        .requestFactory(
            ClientHttpRequestFactoryBuilder.jdk()
                .build(
                    HttpClientSettings.defaults()
                        .withConnectTimeout(Duration.ofSeconds(5))
                        .withReadTimeout(Duration.ofSeconds(10))))
        .build();
  }
}
