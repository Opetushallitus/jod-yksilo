/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Sends education extraction requests to SQS. */
@Component
@Profile("cloud")
@ConditionalOnProperty(
    name = "spring.cloud.aws.sqs.enabled",
    havingValue = "true",
    matchIfMissing = true)
@ConditionalOnProperty(name = "jod.koski.extraction.request-queue")
@Slf4j
class SqsKoskiExtractionSender implements KoskiExtractionSender {

  private final SqsTemplate sqsTemplate;
  private final String requestQueue;

  SqsKoskiExtractionSender(
      SqsTemplate sqsTemplate,
      @Value("${jod.koski.extraction.request-queue}") String requestQueue) {
    this.sqsTemplate = sqsTemplate;
    this.requestQueue = requestQueue;
  }

  @Override
  public void send(KoskiExtractionRequest request) {
    sqsTemplate.send(
        to -> to.queue(requestQueue).messageGroupId(request.userId().toString()).payload(request));
    log.debug("Sent Koski extraction request for task {}", request.taskId());
  }
}
