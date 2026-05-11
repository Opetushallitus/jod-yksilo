/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili.cv;

import fi.okm.jod.yksilo.domain.CvTehtavaTila;
import fi.okm.jod.yksilo.repository.CvTehtavaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Shared handler for processing CV extraction responses. */
@Component
@RequiredArgsConstructor
@Slf4j
class CvResponseHandler {

  private final CvTehtavaRepository tehtavat;
  private final CvResponseMapper cvResponseMapper;

  @Transactional
  void handleResponse(CvResponseMessage message) {
    var tehtava = tehtavat.findByIdAndYksiloId(message.taskId(), message.userId()).orElse(null);
    if (tehtava == null) {
      log.warn("Received CV response for unknown task {}, discarding", message.taskId());
      return;
    }
    try (var _ = MDC.putCloseable("userId", tehtava.getYksilo().getId().toString())) {
      if (tehtava.getTila() != CvTehtavaTila.ODOTTAA) {
        log.warn(
            "Received CV response for task {} in state {}, discarding",
            message.taskId(),
            tehtava.getTila());
        return;
      }
      if (!"SUCCESS".equals(message.status()) || message.response() == null) {
        tehtava.setTila(CvTehtavaTila.EPAONNISTUNUT);
      } else {
        tehtava.setTila(CvTehtavaTila.VALMIS);
        tehtava.setTulos(cvResponseMapper.toTulos(message.response(), tehtava.getKieli()));
      }
      log.info(
          "Processed CV extraction response for task {}: {}", message.taskId(), tehtava.getTila());
    }
  }
}
