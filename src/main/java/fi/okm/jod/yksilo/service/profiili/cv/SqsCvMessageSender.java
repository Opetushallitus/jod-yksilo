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
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Sends CV extraction request messages to SQS. Only active in cloud profile. */
@Component
@Profile("cloud")
@ConditionalOnProperty(
    name = "spring.cloud.aws.sqs.enabled",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
class SqsCvMessageSender implements CvMessageSender {

  private final SqsTemplate sqsTemplate;
  private final CvProperties properties;

  @Override
  public void send(CvRequestMessage message) {
    sqsTemplate.send(properties.requestQueue(), message);
    log.debug("Sent CV extraction request to SQS for task {}", message.taskId());
  }
}
