/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.event;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.OsaamisenTunnistusStatus;
import fi.okm.jod.yksilo.service.inference.InferenceService;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class OsaamisetTunnistusEventHandler {

  private final KoulutusService koulutusService;
  private final String aiTunnistusOsaamisetEndpoint;
  private final InferenceService<SageMakerRequest, SageMakerResponse> inferenceService;
  private final Semaphore semaphore;

  public OsaamisetTunnistusEventHandler(
      KoulutusService koulutusService,
      @Value("${jod.ai-tunnistus.osaamiset.endpoint}") String aiTunnistusOsaamisetEndpoint,
      @Value("${jod.ai-tunnistus.osaamiset.max-concurrent-requests:4}") int maxConcurrentRequests,
      InferenceService<SageMakerRequest, SageMakerResponse> inferenceService) {
    if (maxConcurrentRequests <= 0) {
      throw new IllegalArgumentException("maxConcurrentRequests must be greater than 0");
    }

    this.koulutusService = koulutusService;
    this.aiTunnistusOsaamisetEndpoint = aiTunnistusOsaamisetEndpoint;
    this.inferenceService = inferenceService;
    this.semaphore = new Semaphore(maxConcurrentRequests);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  @SuppressWarnings("try")
  public void handleOsaamisetTunnistusEvent(OsaamisetTunnistusEvent event) {
    try (var ignored = MDC.putCloseable("userId", event.jodUser().getId().toString())) {
      doHandleOsaamisetTunnistusEvent(event);
    }
  }

  @SuppressWarnings("java:S2142")
  void doHandleOsaamisetTunnistusEvent(OsaamisetTunnistusEvent event) {

    log.info("Processing OsaamisetTunnistusEvent");

    var interrupted = false;
    var koulutukset = new ArrayDeque<>(event.koulutukset());

    while (!koulutukset.isEmpty() && !interrupted) {
      // Limit concurrent processing to avoid overwhelming the AI service
      // Using a semaphore is OK given we are using virtual threads
      try {
        semaphore.acquire();
      } catch (InterruptedException e) {
        interrupted = true;
        break;
      }
      var koulutus = koulutukset.pop();
      try {
        var osaamiset = inferOsaamiset(koulutus);
        koulutusService.completeOsaamisetTunnistus(
            koulutus, OsaamisenTunnistusStatus.DONE, osaamiset);
      } catch (Exception e) {
        log.error("Failed to process OsaamisetTunnistusEvent", e);
        if (Thread.interrupted()) {
          interrupted = true;
        }
        koulutusService.completeOsaamisetTunnistus(koulutus, OsaamisenTunnistusStatus.FAIL, null);
      } finally {
        semaphore.release();
      }
    }
    try {
      for (Koulutus koulutus : koulutukset) {
        log.warn("Failed to process OsaamisetTunnistusEvent for koulutus {} ", koulutus.getId());
        koulutusService.completeOsaamisetTunnistus(koulutus, OsaamisenTunnistusStatus.FAIL, null);
      }
    } finally {
      if (interrupted) {
        log.warn("OsaamisetTunnistusEvent processing was interrupted.");
        Thread.currentThread().interrupt(); // Restore interrupted status
      }
    }
  }

  private Set<URI> inferOsaamiset(Koulutus koulutus) {
    var request =
        new SageMakerRequest(
            koulutus.getId(), koulutus.getNimi().get(Kieli.FI), koulutus.getOsasuoritukset());

    return inferenceService
        .infer(aiTunnistusOsaamisetEndpoint, request, new ParameterizedTypeReference<>() {})
        .osaamiset();
  }

  public record SageMakerRequest(UUID id, String nimi, Set<String> osasuoritukset) {}

  public record SageMakerResponse(UUID id, Set<URI> osaamiset) {}
}
