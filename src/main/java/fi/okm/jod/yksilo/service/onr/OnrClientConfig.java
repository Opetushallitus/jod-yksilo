/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.onr;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "jod.onr.base-url")
public class OnrClientConfig {

  private static final String REGISTRATION_ID = "onr";

  private static ClientRegistration createClientRegistration(OnrConfiguration config) {
    var oauth2 = config.getOauth2();
    return ClientRegistration.withRegistrationId(REGISTRATION_ID)
        .clientId(oauth2.clientId())
        .clientSecret(oauth2.clientSecret())
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .tokenUri(oauth2.tokenUri())
        .build();
  }

  private static OAuth2AuthorizedClientManager onrAuthorizedClientManager(OnrConfiguration config) {
    var registration = createClientRegistration(config);
    var registrations = new InMemoryClientRegistrationRepository(registration);
    var service = new InMemoryOAuth2AuthorizedClientService(registrations);
    var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, service);
    var provider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();
    manager.setAuthorizedClientProvider(provider);
    return manager;
  }

  @Bean("onrRestClient")
  RestClient onrRestClient(RestClient.Builder builder, OnrConfiguration config) {
    var onrAuthorizedClientManager = onrAuthorizedClientManager(config);
    return builder
        .baseUrl(config.getBaseUrl())
        .requestInterceptor(
            (request, body, execution) -> {
              var authorizeRequest =
                  OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                      .principal("onr-service")
                      .build();
              var client = onrAuthorizedClientManager.authorize(authorizeRequest);
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
