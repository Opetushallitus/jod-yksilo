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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;

@Slf4j
@ConditionalOnBean(KoskiOauth2Config.class)
@Configuration(proxyBeanMethods = false)
public class KoskiRestClientConfig {

  public static final String OAUTH2_RESTCLIENT_ID = "koskiOAuth2RestClient";
  public static final String RESTCLIENT_ID = "koskiRestClient";

  @Bean
  @RefreshScope
  SslBundle koskiClientSslBundle(KoskiCertificateProperties koskiCertificateProperties) {
    return SslBundle.of(
        new PemSslStoreBundle(
            PemSslStoreDetails.forCertificate(koskiCertificateProperties.getFullChain())
                .withPrivateKey(koskiCertificateProperties.getPrivateKey()),
            null /* default truststore */));
  }

  @Bean(RESTCLIENT_ID)
  @RefreshScope
  public RestClient koskiRestClient(
      RestClient.Builder builder,
      MappingJackson2HttpMessageConverter messageConverter,
      SslBundle koskiSslBundle) {
    return createRestClient(builder, koskiSslBundle)
        .messageConverters(List.of(messageConverter))
        .build();
  }

  @Bean(OAUTH2_RESTCLIENT_ID)
  @RefreshScope
  public RestClient koskiRestClientOauth2(RestClient.Builder builder, SslBundle koskiSslBundle) {
    var messageConverters =
        List.of(
            new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter());
    return createRestClient(builder, koskiSslBundle).messageConverters(messageConverters).build();
  }

  private static RestClient.Builder createRestClient(
      RestClient.Builder builder, SslBundle koskiSslBundle) {
    var requestFactory =
        ClientHttpRequestFactoryBuilder.jdk()
            .build(
                ClientHttpRequestFactorySettings.defaults()
                    .withSslBundle(koskiSslBundle)
                    .withConnectTimeout(Duration.ofSeconds(10))
                    .withReadTimeout(Duration.ofSeconds(30)));

    return builder
        .requestFactory(requestFactory)
        .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler());
  }
}
