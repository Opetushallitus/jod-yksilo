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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;

@ConditionalOnBean(KoskiOAuth2Config.class)
@Configuration(proxyBeanMethods = false)
public class KoskiRestClientConfig {

  public static final String OAUTH2_RESTCLIENT_ID = "koskiOAuth2RestClient";
  public static final String RESTCLIENT_ID = "koskiRestClient";

  private static final String SSL_BUNDLE = "koski-ssl-bundle";

  @Bean(RESTCLIENT_ID)
  public RestClient koskiRestClient(
      RestClient.Builder builder,
      MappingJackson2HttpMessageConverter messageConverter,
      SslBundles sslBundles) {
    return createRestClient(builder, sslBundles)
        .messageConverters(List.of(messageConverter))
        .build();
  }

  @Bean(OAUTH2_RESTCLIENT_ID)
  public RestClient koskiRestClientOAuth2(RestClient.Builder builder, SslBundles sslBundles) {
    var messageConverters =
        List.of(
            new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter());
    return createRestClient(builder, sslBundles).messageConverters(messageConverters).build();
  }

  private static RestClient.Builder createRestClient(
      RestClient.Builder builder, SslBundles sslBundles) {
    var requestFactory =
        ClientHttpRequestFactoryBuilder.jdk()
            .build(
                ClientHttpRequestFactorySettings.defaults()
                    .withSslBundle(sslBundles.getBundle(SSL_BUNDLE))
                    .withConnectTimeout(Duration.ofSeconds(10))
                    .withReadTimeout(Duration.ofSeconds(30)));

    return builder
        .requestFactory(requestFactory)
        .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler());
  }
}
