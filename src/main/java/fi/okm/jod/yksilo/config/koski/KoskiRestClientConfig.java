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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

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
      RestClient.Builder builder, SslBundle koskiSslBundle, JsonMapper.Builder mapperBuilder) {
    return createRestClient(builder, koskiSslBundle)
        .configureMessageConverters(
            converters ->
                converters.withJsonConverter(new JacksonJsonHttpMessageConverter(mapperBuilder)))
        .build();
  }

  @Bean(OAUTH2_RESTCLIENT_ID)
  @RefreshScope
  public RestClient koskiRestClientOauth2(RestClient.Builder builder, SslBundle koskiSslBundle) {
    return createRestClient(builder, koskiSslBundle)
        .configureMessageConverters(
            converters ->
                converters
                    .addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter())
                    .addCustomConverter(new FormHttpMessageConverter()))
        .build();
  }

  private static RestClient.Builder createRestClient(
      RestClient.Builder builder, SslBundle koskiSslBundle) {
    var requestFactory =
        ClientHttpRequestFactoryBuilder.jdk()
            .build(
                HttpClientSettings.defaults()
                    .withSslBundle(koskiSslBundle)
                    .withConnectTimeout(Duration.ofSeconds(10))
                    .withReadTimeout(Duration.ofSeconds(30)));

    return builder
        .requestFactory(requestFactory)
        .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler());
  }
}
