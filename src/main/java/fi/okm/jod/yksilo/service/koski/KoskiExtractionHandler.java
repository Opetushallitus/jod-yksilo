/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import fi.okm.jod.yksilo.domain.KoskiTehtavaTila;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.repository.KoskiTehtavaRepository;
import fi.okm.jod.yksilo.service.OsaaminenService;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Completes a waiting Koski task with the skills from the education extraction. */
@Component
@Slf4j
class KoskiExtractionHandler {

  private final KoskiTehtavaRepository tehtavat;
  private final OsaaminenService osaaminenService;
  private final TransactionTemplate transactionTemplate;

  KoskiExtractionHandler(
      KoskiTehtavaRepository tehtavat,
      OsaaminenService osaaminenService,
      PlatformTransactionManager transactionManager) {
    this.tehtavat = tehtavat;
    this.osaaminenService = osaaminenService;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  void handleResponse(KoskiExtractionResponse message) {
    try {
      transactionTemplate.executeWithoutResult(_ -> complete(message));
    } catch (ObjectOptimisticLockingFailureException _) {
      log.info(
          "Koski task {} changed while its extraction finished, discarding the result",
          message.taskId());
    }
  }

  private void complete(KoskiExtractionResponse message) {
    var tehtava = tehtavat.findByIdAndYksiloId(message.taskId(), message.userId()).orElse(null);
    if (tehtava == null) {
      log.warn(
          "Received Koski extraction response for unknown task {}, discarding", message.taskId());
      return;
    }
    try (var _ = MDC.putCloseable("userId", message.userId().toString())) {
      if (tehtava.getTila() != KoskiTehtavaTila.ODOTTAA) {
        log.info(
            "Received Koski extraction response for task {} in state {}, discarding",
            message.taskId(),
            tehtava.getTila());
        return;
      }
      if (KoskiExtractionResponse.SUCCESS.equals(message.status()) && message.osaamiset() != null) {
        tehtava.setTulos(withSkills(tehtava.getTulos(), message.osaamiset()));
      }
      tehtava.finish();
      log.info(
          "Processed Koski extraction response for task {}: {}",
          message.taskId(),
          tehtava.getTila());
    }
  }

  /** The educations with their extracted skills; skills missing from the taxonomy are ignored. */
  private KoskiTehtavaDto.Tulos withSkills(
      KoskiTehtavaDto.Tulos tulos, Map<UUID, List<URI>> osaamiset) {
    var known = osaaminenService.getAll().keySet();
    return new KoskiTehtavaDto.Tulos(
        tulos.koulutuskokonaisuudet().stream()
            .map(
                k ->
                    new KoulutusKokonaisuusDto(
                        k.id(),
                        k.nimi(),
                        k.tuontiLahde(),
                        k.koulutukset().stream()
                            .map(
                                koulutus ->
                                    withSkills(koulutus, osaamiset.get(koulutus.id()), known))
                            .collect(Collectors.toCollection(LinkedHashSet::new))))
            .toList());
  }

  private static KoulutusDto withSkills(KoulutusDto koulutus, List<URI> skills, Set<URI> known) {
    if (skills == null) {
      return koulutus;
    }
    return new KoulutusDto(
        koulutus.id(),
        koulutus.nimi(),
        koulutus.kuvaus(),
        koulutus.alkuPvm(),
        koulutus.loppuPvm(),
        skills.stream()
            .filter(known::contains)
            .collect(Collectors.toCollection(LinkedHashSet::new)));
  }
}
