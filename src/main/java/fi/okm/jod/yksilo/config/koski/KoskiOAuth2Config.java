/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.koski;

import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;

@Getter
@Slf4j
@ConditionalOnProperty(
    name = "spring.security.oauth2.client.registration.koski.provider",
    havingValue = "koski-mtls")
@Configuration(proxyBeanMethods = false)
public class KoskiOAuth2Config {

  public static final String RESTCLIENT_ID = "koski-rest-client";
  private static final String REGISTRATION_ID = "koski";
  private static final String SSL_BUNDLE = "koski-ssl-bundle";

  private final String resourceServer;

  public KoskiOAuth2Config(@Value("${jod.koski.resource-server.url}") String resourceServer) {
    this.resourceServer = resourceServer;
  }

  @Bean(RESTCLIENT_ID)
  public RestClient koskiRestClient(
      RestClient.Builder builder,
      OAuth2AuthorizedClientManager authorizedClientManager,
      SslBundles sslBundles) {
    var requestFactory =
        ClientHttpRequestFactoryBuilder.jdk()
            .build(
                ClientHttpRequestFactorySettings.defaults()
                    .withSslBundle(sslBundles.getBundle(SSL_BUNDLE))
                    .withConnectTimeout(Duration.ofSeconds(5))
                    .withReadTimeout(Duration.ofSeconds(10)));

    var messageConverters =
        List.of(
            new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter(),
            new StringHttpMessageConverter(), new MappingJackson2HttpMessageConverter());

    return builder
        .requestInterceptor(new OAuth2ClientHttpRequestInterceptor(authorizedClientManager))
        .requestFactory(requestFactory)
        .messageConverters(messageConverters)
        .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
        .build();
  }

  public String getRegistrationId() {
    return REGISTRATION_ID;
  }
}
