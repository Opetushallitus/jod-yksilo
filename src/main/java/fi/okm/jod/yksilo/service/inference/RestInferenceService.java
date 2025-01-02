/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.inference;

import fi.okm.jod.yksilo.service.ServiceException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Profile("default")
public class RestInferenceService<T, R> implements InferenceService<T, R> {

  private final RestClient restClient;

  public RestInferenceService(
      RestClient.Builder restClientBuilder, MappingJackson2HttpMessageConverter messageConverter) {

    var requestFactory =
        ClientHttpRequestFactoryBuilder.jdk()
            .withHttpClientCustomizer(builder -> builder.connectTimeout(Duration.ofMillis(5000)))
            .withCustomizer(c -> c.setReadTimeout(Duration.ofMillis(5000)))
            .build();

    this.restClient =
        restClientBuilder
            .requestFactory(requestFactory)
            .messageConverters(
                converters -> {
                  converters.clear();
                  converters.add(messageConverter);
                })
            .build();
  }

  @Override
  public R infer(String endpoint, T payload, Class<R> responseType) {
    try {
      return restClient.post().uri(endpoint).body(payload).retrieve().body(responseType);
    } catch (RestClientException e) {
      throw new ServiceException("Invoking Inference Endpoint failed", e);
    }
  }

  @Override
  public InferenceSession<R> infer(
      String endpoint, UUID sessionId, T payload, Class<R> responseType) {
    throw new UnsupportedOperationException("Session is not supported by this inference service");
  }
}
