/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.koski;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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

@ConditionalOnBean(KoskiOAuth2Config.class)
@Configuration(proxyBeanMethods = false)
public class KoskiRestClientConfig {

  public static final String RESTCLIENT_ID = "koski-rest-client";

  private static final String SSL_BUNDLE = "koski-ssl-bundle";

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
}
