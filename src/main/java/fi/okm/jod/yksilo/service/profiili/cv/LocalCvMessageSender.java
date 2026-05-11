/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili.cv;

import fi.okm.jod.yksilo.config.CvProperties;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "false")
@Slf4j
class LocalCvMessageSender implements CvMessageSender {

  private final RestClient restClient;
  private final CvResponseHandler responseHandler;

  LocalCvMessageSender(
      CvResponseHandler responseHandler,
      CvProperties properties,
      RestClient.Builder restClientBuilder) {
    this.responseHandler = responseHandler;
    if (properties.requestQueue() == null || !properties.requestQueue().startsWith("http:")) {
      log.info(
          "No local request queue defined, LocalCvMessageSender will simulate failed requests");
      this.restClient = null;
    } else {
      log.info("Initializing LocalCvMessageSender with endpoint: {}", properties.requestQueue());
      this.restClient = restClientBuilder.baseUrl(properties.requestQueue()).build();
    }
  }

  @Override
  @Async
  public void send(CvRequestMessage message) {

    if (restClient == null) {
      log.error("Local CV extraction failed for task {}", message.taskId());
      responseHandler.handleResponse(failed(message));
      return;
    }

    log.info(
        "Sending CV extraction request to local API for task {} (file: {})",
        message.taskId(),
        message.s3Key());
    try {
      var response =
          restClient
              .post()
              .uri("/extract")
              .contentType(MediaType.APPLICATION_JSON)
              .body(message)
              .retrieve()
              .requiredBody(CvResponseMessage.class);
      responseHandler.handleResponse(response);
    } catch (Exception e) {
      log.error("Local CV extraction failed for task {}", message.taskId(), e);
      responseHandler.handleResponse(failed(message));
    }
  }

  private static @NonNull CvResponseMessage failed(CvRequestMessage message) {
    return new CvResponseMessage(message.userId(), message.taskId(), "FAILED", null);
  }
}
