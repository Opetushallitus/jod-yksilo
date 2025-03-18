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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
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

  public OsaamisetTunnistusEventHandler(
      KoulutusService koulutusService,
      @Value("${jod.ai-tunnistus.osaamiset.endpoint}") String aiTunnistusOsaamisetEndpoint,
      InferenceService<SageMakerRequest, SageMakerResponse> inferenceService) {
    this.koulutusService = koulutusService;
    this.aiTunnistusOsaamisetEndpoint = aiTunnistusOsaamisetEndpoint;
    this.inferenceService = inferenceService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  public CompletableFuture<Void> handleOsaamisetTunnistusEvent(OsaamisetTunnistusEvent event) {
    log.debug("Osaamiset tunnistus event: {}", event);
    var koulutukset = event.koulutukset();
    for (var koulutus : koulutukset) {
      try {
        var osaamisetTunnistusResponses = callIdentifyOsaamisetApi(koulutus);
        koulutusService.updateOsaamisetTunnistusStatus(
            koulutus, OsaamisenTunnistusStatus.DONE, osaamisetTunnistusResponses.osaamiset());

      } catch (Exception e) {
        if (e instanceof IdentifyOsaamisetException) {
          log.error(e.getMessage(), e);
        } else {
          log.error("Fail to processing OsaamisetTunnistusEvent.", e);
        }
        koulutusService.updateOsaamisetTunnistusStatus(
            koulutus, OsaamisenTunnistusStatus.FAIL, null);
      }
    }
    return CompletableFuture.completedFuture(null);
  }

  private SageMakerResponse callIdentifyOsaamisetApi(Koulutus koulutus) {
    try {
      var request =
          new SageMakerRequest(
              koulutus.getId(), koulutus.getNimi().get(Kieli.FI), koulutus.getOsasuoritukset());

      return inferenceService.infer(
          aiTunnistusOsaamisetEndpoint, request, new ParameterizedTypeReference<>() {});

    } catch (Exception e) {
      throw new IdentifyOsaamisetException("Error calling OsaamisetTunnistus AI API.", e);
    }
  }

  record SageMakerRequest(UUID id, String nimi, Set<String> osasuoritukset) {}

  record SageMakerResponse(UUID id, Set<URI> osaamiset) {}

  private static class IdentifyOsaamisetException extends RuntimeException {
    public IdentifyOsaamisetException(String message, Exception exception) {
      super(message, exception);
    }
  }
}
