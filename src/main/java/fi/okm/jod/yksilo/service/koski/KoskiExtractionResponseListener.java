/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Completes Koski tasks with the responses of the education extraction service. Present whenever
 * requests are sent, so a missing response queue fails at startup rather than leaving tasks
 * waiting.
 */
@Component
@Profile("cloud")
@ConditionalOnProperty(
    name = "spring.cloud.aws.sqs.enabled",
    havingValue = "true",
    matchIfMissing = true)
@ConditionalOnProperty(name = "jod.koski.extraction.request-queue")
@RequiredArgsConstructor
class KoskiExtractionResponseListener {

  private final KoskiExtractionHandler handler;

  @SqsListener(value = "${jod.koski.extraction.response-queue}")
  void receive(KoskiExtractionResponse message) {
    handler.handleResponse(message);
  }
}
