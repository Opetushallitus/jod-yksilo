/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili.cv;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Listens for CV extraction response messages from SQS and updates the task status in DB. */
@Component
@Profile("cloud")
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
class CvResponseListener {

  private final CvResponseHandler responseHandler;

  @SqsListener(value = "${jod.cv.response-queue}")
  void receive(CvResponseMessage message) {
    responseHandler.handleResponse(message);
  }
}
