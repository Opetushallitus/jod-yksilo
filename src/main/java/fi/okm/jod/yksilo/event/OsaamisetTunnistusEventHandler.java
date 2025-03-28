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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final InferenceService<List<SageMakerRequestRow>, List<SageMakerResponseRow>>
      inferenceService;

  public OsaamisetTunnistusEventHandler(
      KoulutusService koulutusService,
      @Value("${jod.ai-tunnistus.osaamiset.endpoint}") String aiTunnistusOsaamisetEndpoint,
      InferenceService<List<SageMakerRequestRow>, List<SageMakerResponseRow>> inferenceService) {
    this.koulutusService = koulutusService;
    this.aiTunnistusOsaamisetEndpoint = aiTunnistusOsaamisetEndpoint;
    this.inferenceService = inferenceService;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  public void handleOsaamisetTunnistusEvent(OsaamisetTunnistusEvent event) {
    log.debug("Osaamiset tunnistus event: {}", event);
    var koulutukset = event.koulutukset();
    try {
      List<SageMakerResponseRow> osaamisetTunnistusResponses =
          callIdentifyOsaamisetApi(koulutukset);
      koulutusService.updateOsaamisetTunnistusStatus(
          koulutukset, OsaamisenTunnistusStatus.DONE, convertResponse(osaamisetTunnistusResponses));

    } catch (Exception e) {
      if (e instanceof IdentifyOsaamisetException) {
        log.error(e.getMessage(), e);
      } else {
        log.error("Fail to processing OsaamisetTunnistusEvent.", e);
      }
      koulutusService.updateOsaamisetTunnistusStatus(
          koulutukset, OsaamisenTunnistusStatus.FAIL, null);
    }
  }

  private List<SageMakerResponseRow> callIdentifyOsaamisetApi(List<Koulutus> koulutukset) {
    try {
      List<SageMakerRequestRow> osaamisetTunnistusRequests =
          koulutukset.stream()
              .map(
                  koulutus ->
                      new SageMakerRequestRow(
                          koulutus.getId(),
                          koulutus.getNimi().get(Kieli.FI),
                          koulutus.getOsasuoritukset()))
              .toList();

      return inferenceService.infer(
          aiTunnistusOsaamisetEndpoint,
          osaamisetTunnistusRequests,
          new ParameterizedTypeReference<>() {});

    } catch (Exception e) {
      throw new IdentifyOsaamisetException("Error calling OsaamisetTunnistus AI API.", e);
    }
  }

  private Map<UUID, Set<URI>> convertResponse(List<SageMakerResponseRow> response) {
    return response != null
        ? response.stream()
            .collect(Collectors.toMap(response1 -> response1.id, response1 -> response1.osaamiset))
        : null;
  }

  public record SageMakerRequestRow(UUID id, String nimi, Set<String> osaamiset) {}

  public record SageMakerResponseRow(UUID id, Set<URI> osaamiset) {}

  private static class IdentifyOsaamisetException extends RuntimeException {
    public IdentifyOsaamisetException(String message, Exception exception) {
      super(message, exception);
    }
  }
}
