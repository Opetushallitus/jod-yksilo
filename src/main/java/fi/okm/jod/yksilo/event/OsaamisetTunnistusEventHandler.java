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
import fi.okm.jod.yksilo.repository.KoulutusRepository;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class OsaamisetTunnistusEventHandler {

  private static final String IDENTIFY_OSAAMISET_API_URI =
      "/yksilo/api/tunnistus/osaamiset"; // TODO: OPHJOD-1403

  private final KoulutusRepository koulutusRepository;
  private final RestClient restClient;
  private final YksilonOsaaminenService osaaminenService;

  public OsaamisetTunnistusEventHandler(
      KoulutusRepository koulutusRepository,
      @Value("${jod.ai.osaamiset.url}") String aiOsaamisetTunnistusUrl,
      RestClient.Builder restClientBuilder,
      YksilonOsaaminenService osaaminenService) {
    this.koulutusRepository = koulutusRepository;
    this.osaaminenService = osaaminenService;
    var requestFactory =
        ClientHttpRequestFactoryBuilder.jdk()
            .build(
                ClientHttpRequestFactorySettings.defaults()
                    .withConnectTimeout(Duration.ofSeconds(10))
                    .withReadTimeout(Duration.ofSeconds(30)));
    this.restClient =
        restClientBuilder.baseUrl(aiOsaamisetTunnistusUrl).requestFactory(requestFactory).build();
  }

  @EventListener
  @Async
  public void handleOsaamisetTunnistusEvent(OsaamisetTunnistusEvent event) {
    log.debug("Osaamiset tunnistus event: {}", event);
    var koulutukset = event.koulutukset();
    try {
      List<OsaamisetTunnistusResponse> osaamisetTunnistusResponses =
          callIdentifyOsaamisetApi(koulutukset);
      updateOsaamisetTunnistusStatus(
          koulutukset, OsaamisenTunnistusStatus.DONE, osaamisetTunnistusResponses);

    } catch (Exception e) {
      if (e instanceof IdentifyOsaamisetException) {
        log.error(e.getMessage(), e);
      } else {
        log.error("Error processing OsaamisetTunnistusEvent: {}", e.getMessage(), e);
      }
      updateOsaamisetTunnistusStatus(koulutukset, OsaamisenTunnistusStatus.FAIL, null);
    }
  }

  private List<OsaamisetTunnistusResponse> callIdentifyOsaamisetApi(List<Koulutus> koulutukset) {
    try {
      List<OsaamisetTunnistusRequest> osaamisetTunnistusRequests =
          koulutukset.stream()
              .map(
                  koulutus ->
                      new OsaamisetTunnistusRequest(
                          koulutus.getId(),
                          koulutus.getNimi().get(Kieli.FI),
                          koulutus.getOsasuoritukset()))
              .toList();

      return restClient
          .post()
          .uri(IDENTIFY_OSAAMISET_API_URI)
          .body(osaamisetTunnistusRequests)
          .retrieve()
          .body(new ParameterizedTypeReference<>() {});

    } catch (Exception e) {
      throw new IdentifyOsaamisetException("Error calling OsaamisetTunnistus AI API.", e);
    }
  }

  @Transactional(Transactional.TxType.REQUIRED)
  protected void updateOsaamisetTunnistusStatus(
      List<Koulutus> koulutukset,
      OsaamisenTunnistusStatus newStatus,
      @Nullable List<OsaamisetTunnistusResponse> apiResponse) {
    List<Koulutus> latestKoulutukset =
        koulutusRepository.findAllById(koulutukset.stream().map(Koulutus::getId).toList());
    latestKoulutukset.forEach(koulutus -> koulutus.setOsaamisenTunnistusStatus(newStatus));

    if (apiResponse != null) {
      Map<UUID, Set<URI>> koulutusIdToOsaamisetMap =
          apiResponse.stream()
              .collect(Collectors.toMap(response -> response.id, response -> response.osaamiset));
      if (!koulutusIdToOsaamisetMap.isEmpty()) {
        latestKoulutukset.forEach(
            koulutus -> {
              Set<URI> osaamisetUris = koulutusIdToOsaamisetMap.get(koulutus.getId());
              if (osaamisetUris != null && !osaamisetUris.isEmpty()) {
                osaaminenService.add(koulutus, osaamisetUris);
              }
            });
      }
    }

    koulutusRepository.saveAllAndFlush(latestKoulutukset);
  }

  @Getter
  @AllArgsConstructor
  public static class OsaamisetTunnistusRequest {
    private UUID id;
    private String nimi;
    private Set<String> osaamiset;
  }

  @Getter
  @AllArgsConstructor
  public static class OsaamisetTunnistusResponse {
    private UUID id;
    private Set<URI> osaamiset;
  }

  private static class IdentifyOsaamisetException extends RuntimeException {
    public IdentifyOsaamisetException(String message, Exception exception) {
      super(message, exception);
    }
  }
}
