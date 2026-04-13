/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.onr;

import fi.okm.jod.yksilo.domain.OppijanumeroUtils;
import fi.okm.jod.yksilo.service.onr.OppijanumeroService.TuontiResult.Tiedot;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@ConditionalOnProperty(name = "jod.onr.base-url")
public class OppijanumeroService {

  private static final long MAX_WAIT_TIME = Duration.ofSeconds(3).toNanos();
  private final RestClient restClient;
  private final String oidPrefix;

  public OppijanumeroService(
      @Qualifier("onrRestClient") RestClient restClient, OnrConfiguration config) {
    this.restClient = restClient;
    this.oidPrefix = config.getOidPrefix();
    log.info(
        "Initialized OppijanumeroService with prefix {} and base URL {}",
        this.oidPrefix,
        config.getBaseUrl());
  }

  /**
   * Fetches the oppijanumero (learner ID) for a person via the ONR yleistunniste API.
   *
   * @return ONR:-prefixed oppijanumero
   * @throws OppijanumeroServiceException on connectivity or authentication failure
   * @see <a
   *     href="https://virkailija.testiopintopolku.fi/oppijanumerorekisteri-service/swagger-ui/">ONR
   *     API</a>
   */
  public Optional<String> fetchOppijanumero(
      String hetu, String etunimet, String kutsumanimi, String sukunimi) {
    try {
      log.info("Fetching oppijanumero from ONR");
      var correlationId = UUID.randomUUID().toString();
      var result =
          restClient
              .put()
              .uri("/yleistunniste")
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  new YleistunnisteInput(
                      List.of(
                          new Tunniste(
                              correlationId, new Henkilo(etunimet, kutsumanimi, sukunimi, hetu)))))
              .retrieve()
              .body(OppijatuontiPerustiedotDto.class);

      if (result == null) {
        throw new OppijanumeroServiceException(
            "Failed to fetch oppijanumero from ONR: empty response");
      }

      String oppijanumero = null;
      var start = System.nanoTime();
      for (var retries = 5; retries > 0; retries--) {
        try {
          var tuontiResult =
              restClient
                  .get()
                  .uri("/yleistunniste/tuonti={id}", result.id())
                  .retrieve()
                  .body(TuontiResult.class);

          if (tuontiResult != null && tuontiResult.kasitelty()) {
            if (tuontiResult.henkilot() != null && !tuontiResult.henkilot().isEmpty()) {
              if (tuontiResult.henkilot().getFirst() instanceof Tiedot tiedot
                  && correlationId.equals(tiedot.tunniste())) {
                if (tiedot.henkilo() != null) {
                  oppijanumero = tiedot.henkilo().oppijanumero();
                  break;
                }
              }
            }
            throw new OppijanumeroServiceException(
                "Failed to fetch oppijanumero from ONR: unexpected response");
          }
        } catch (HttpClientErrorException.Forbidden e) {
          // it seems a request can fail if we are too fast (zero TuontiRivi == 403)
          log.info("Received 403 Forbidden from ONR: {}", e.getMessage());
        }

        var elapsed = (System.nanoTime() - start);
        if (elapsed > MAX_WAIT_TIME) {
          log.info("Exceeded max wait time for ONR response after {} ms", elapsed / 1_000_000);
          break;
        }

        try {
          Thread.sleep(500);
        } catch (InterruptedException _) {
          Thread.currentThread().interrupt();
          log.info("Interrupted while waiting for ONR response");
          break;
        }
      }

      if (oppijanumero == null) {
        log.info("ONR did not return oppijanumero within expected time");
        return Optional.empty();
      }

      log.info("Got response from ONR: {}", oppijanumero);
      return Optional.of(OppijanumeroUtils.qualify(oppijanumero, oidPrefix));

    } catch (RestClientException e) {
      throw new OppijanumeroServiceException("Failed to fetch oppijanumero from ONR", e);
    } catch (IllegalArgumentException e) {
      throw new OppijanumeroServiceException("ONR returned invalid oppijanumero", e);
    }
  }

  record YleistunnisteInput(List<Tunniste> henkilot) {}

  record Tunniste(String tunniste, Henkilo henkilo) {}

  record Henkilo(String etunimet, String kutsumanimi, String sukunimi, String hetu) {}

  record OppijatuontiPerustiedotDto(long id) {}

  record TuontiResult(long id, boolean kasitelty, List<Tiedot> henkilot) {
    record Tiedot(String tunniste, Oppija henkilo, boolean conflict) {}

    record Oppija(String oid, String oppijanumero, boolean passivoitu) {}
  }
}
