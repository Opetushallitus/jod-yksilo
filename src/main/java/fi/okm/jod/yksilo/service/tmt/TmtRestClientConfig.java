/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.tmt;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.Duration;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
class TmtRestClientConfig {

  @Bean
  RestClient tmtExportRestClient(
      RestClient.Builder restClientBuilder, Jackson2ObjectMapperBuilder mapperBuilder) {
    var requestFactory =
        ClientHttpRequestFactoryBuilder.jdk()
            .build(
                ClientHttpRequestFactorySettings.defaults()
                    .withConnectTimeout(Duration.ofSeconds(10))
                    .withReadTimeout(Duration.ofSeconds(30)));
    var objectMapper = mapperBuilder.serializationInclusion(Include.NON_EMPTY).build();

    return restClientBuilder
        .requestFactory(requestFactory)
        .messageConverters(
            converters -> {
              converters.clear();
              converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
            })
        .build();
  }
}
