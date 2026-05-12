/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.onr.task;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Submits bulk oppijanumero requests to the ONR yleistunniste API and polls for results. This
 * service is intentionally not transactional — all DB operations happen in the caller.
 */
@Service
@Slf4j
class OnrUpdateService {

  private final RestClient restClient;
  private final String oidPrefix;
  private final Duration initialPollDelay;
  private final RetryTemplate retryTemplate;

  OnrUpdateService(RestClient restClient, OnrTaskProperties config) {
    this.restClient = restClient;
    this.oidPrefix = config.oidPrefix();
    var retry = config.retry();
    this.initialPollDelay = retry.initialPollDelay();
    this.retryTemplate =
        new RetryTemplate(
            RetryPolicy.builder()
                .maxRetries(retry.maxRetries())
                .delay(retry.delay())
                .maxDelay(retry.maxDelay())
                .timeout(retry.timeout())
                .includes(
                    NotReadyException.class,
                    /* ONR API can return 403 if the result is not ready yet */
                    HttpClientErrorException.Forbidden.class)
                .build());
    log.info(
        "Initialized OnrUpdateService with oidPrefix={}, baseUrl={}", oidPrefix, config.baseUrl());
  }

  /**
   * Submits a batch to ONR and returns a map of yksiloId to qualified oppijanumero for successful
   * lookups. Failed/conflicting entries are logged and omitted.
   */
  Map<UUID, String> processBatch(List<HenkiloRow> batch) {
    // Build the bulk request — use random UUIDs as correlation keys to avoid leaking internal ids
    var correlationMap = new HashMap<String, UUID>(); // correlationId -> yksiloId
    var tunnisteet =
        batch.stream()
            .map(
                row -> {
                  var correlationId = UUID.randomUUID().toString();
                  correlationMap.put(correlationId, row.yksiloId());
                  return new Tunniste(
                      correlationId,
                      new Henkilo(row.etunimi(), row.etunimi(), row.sukunimi(), row.hetu()));
                })
            .toList();

    long tuontiId;
    try {
      var result =
          restClient
              .put()
              .uri("/yleistunniste")
              .contentType(MediaType.APPLICATION_JSON)
              .body(new YleistunnisteInput(tunnisteet))
              .retrieve()
              .requiredBody(OppijatuontiPerustiedotDto.class);
      tuontiId = result.id();
      log.info("Submitted batch of {} to ONR, tuontiId={}", batch.size(), tuontiId);
    } catch (RestClientException e) {
      log.error("Failed to submit batch to ONR", e);
      return Map.of();
    }

    // Poll for results (wait before first poll — result is unlikely to be ready immediately)
    TuontiResult tuontiResult;
    try {
      Thread.sleep(initialPollDelay);
      tuontiResult =
          retryTemplate.execute(
              () -> {
                var response =
                    restClient
                        .get()
                        .uri("/yleistunniste/tuonti={id}", tuontiId)
                        .retrieve()
                        .requiredBody(TuontiResult.class);
                if (!response.kasitelty()) {
                  throw new NotReadyException("ONR has not processed tuonti " + tuontiId + " yet");
                }
                return response;
              });
    } catch (RetryException e) {
      log.error("Failed to poll ONR for tuontiId={}", tuontiId, e);
      return Map.of();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Polling thread interrupted while waiting for ONR tuontiId={}", tuontiId, e);
      return Map.of();
    }

    // Match results back to rows
    return matchResults(tuontiResult, correlationMap);
  }

  private Map<UUID, String> matchResults(TuontiResult result, Map<String, UUID> correlationMap) {
    var matched = new HashMap<UUID, String>();
    if (result.henkilot() == null) {
      return matched;
    }
    for (var tiedot : result.henkilot()) {
      var yksiloId = correlationMap.get(tiedot.tunniste());
      if (yksiloId == null) {
        log.warn("Unexpected tunniste from ONR: {}", tiedot.tunniste());
        continue;
      }

      if (tiedot.conflict()) {
        log.warn("ONR returned conflict for yksiloId={}", yksiloId);
        continue;
      }

      var oppija = tiedot.henkilo();
      if (oppija == null || oppija.oppijanumero() == null || oppija.passivoitu()) {
        log.warn(
            "ONR returned no valid oppijanumero for yksiloId={} (oppija={})", yksiloId, oppija);
        continue;
      }

      try {
        var qualified = OppijanumeroUtils.qualify(oppija.oppijanumero(), oidPrefix);
        matched.put(yksiloId, qualified);
      } catch (IllegalArgumentException e) {
        log.warn("ONR returned invalid oppijanumero for yksiloId={}: {}", yksiloId, e.getMessage());
      }
    }
    return matched;
  }

  static class NotReadyException extends Exception {
    public NotReadyException(String message) {
      super(message);
    }
  }

  // DTOs for ONR API
  record YleistunnisteInput(List<Tunniste> henkilot) {}

  record Tunniste(String tunniste, Henkilo henkilo) {}

  record Henkilo(String etunimet, String kutsumanimi, String sukunimi, String hetu) {}

  record OppijatuontiPerustiedotDto(long id) {}

  record TuontiResult(long id, boolean kasitelty, List<Tiedot> henkilot) {
    record Tiedot(String tunniste, Oppija henkilo, boolean conflict) {}

    record Oppija(String oid, String oppijanumero, boolean passivoitu) {}
  }
}
