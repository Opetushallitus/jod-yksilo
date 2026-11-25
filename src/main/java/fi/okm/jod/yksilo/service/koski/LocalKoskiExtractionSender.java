/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Calls a locally running education extraction service over HTTP in place of SQS. */
@Component
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "false")
@ConditionalOnProperty(name = "jod.koski.extraction.request-queue")
@Slf4j
class LocalKoskiExtractionSender implements KoskiExtractionSender {

  private final RestClient restClient;
  private final KoskiExtractionHandler handler;

  LocalKoskiExtractionSender(
      @Value("${jod.koski.extraction.request-queue}") String requestQueue,
      KoskiExtractionHandler handler,
      RestClient.Builder restClientBuilder) {
    this.restClient = restClientBuilder.baseUrl(requestQueue).build();
    this.handler = handler;
    log.info("Using local Koski education extraction at {}", requestQueue);
  }

  @Override
  @Async
  public void send(KoskiExtractionRequest request) {
    KoskiExtractionResponse response;
    try {
      response =
          restClient
              .post()
              .uri("/extract")
              .contentType(MediaType.APPLICATION_JSON)
              .body(request)
              .retrieve()
              .requiredBody(KoskiExtractionResponse.class);
    } catch (Exception e) {
      log.error("Local Koski extraction failed for task {}", request.taskId(), e);
      response = KoskiExtractionResponse.failed(request.taskId(), request.userId());
    }
    handler.handleResponse(response);
  }
}
