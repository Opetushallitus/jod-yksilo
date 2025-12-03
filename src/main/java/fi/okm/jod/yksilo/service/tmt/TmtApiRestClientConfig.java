/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.tmt;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.web.client.RestClient;

@Configuration
class TmtApiRestClientConfig {
  @Bean
  RestClient tmtRestClient(
      RestClient.Builder builder, MappingJackson2HttpMessageConverter messageConverter) {
    var requestFactory =
        ClientHttpRequestFactoryBuilder.jdk()
            .build(
                ClientHttpRequestFactorySettings.defaults()
                    .withConnectTimeout(Duration.ofSeconds(10))
                    .withReadTimeout(Duration.ofSeconds(30)));

    return builder
        .requestFactory(requestFactory)
        .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
        .messageConverters(List.of(messageConverter))
        .build();
  }
}
