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
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@ConditionalOnProperty(name = "jod.onr.base-url")
public class OppijanumeroService {

  private final RestClient restClient;
  private final String oidPrefix;
  private final RetryTemplate retryTemplate;

  public OppijanumeroService(
      @Qualifier("onrRestClient") RestClient restClient, OnrConfiguration config) {
    this.restClient = restClient;
    this.oidPrefix = config.getOidPrefix();
    this.retryTemplate =
        new RetryTemplate(
            RetryPolicy.builder()
                .maxRetries(4)
                .delay(Duration.ofMillis(500))
                .maxDelay(Duration.ofSeconds(1))
                .timeout(Duration.ofSeconds(5))
                .includes(HttpClientErrorException.Forbidden.class, IllegalStateException.class)
                .build());

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
  public String fetchOppijanumero(
      String hetu, String etunimet, String kutsumanimi, String sukunimi) {
    try {
      var correlationId = UUID.randomUUID().toString();
      log.info("Fetching oppijanumero from ONR, correlationId {}", correlationId);
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
              .requiredBody(OppijatuontiPerustiedotDto.class);
      var onr = fetchOppijanumero(result, correlationId);
      log.info("Successfully fetched oppijanumero from ONR,  correlationId {}", correlationId);
      return onr;
    } catch (RestClientException | IllegalStateException e) {
      throw new OppijanumeroServiceException("Failed to fetch Oppijanumero", e);
    }
  }

  private String fetchOppijanumero(OppijatuontiPerustiedotDto tuonti, String correlationId) {
    try {
      var result =
          retryTemplate.execute(
              () -> {
                var response =
                    restClient
                        .get()
                        .uri("/yleistunniste/tuonti={id}", tuonti.id())
                        .retrieve()
                        .requiredBody(TuontiResult.class);

                if (!response.kasitelty()) {
                  throw new IllegalStateException("ONR has not processed the request yet");
                }
                return response;
              });

      if (result.henkilot() != null
          && !result.henkilot().isEmpty()
          && result.henkilot().getFirst()
              instanceof Tiedot(String tunniste, TuontiResult.Oppija henkilo, boolean conflict)
          && correlationId.equals(tunniste)
          && !conflict
          && henkilo != null
          && henkilo.oppijanumero() != null
          && !henkilo.passivoitu()) {
        return OppijanumeroUtils.qualify(henkilo.oppijanumero(), oidPrefix);
      } else {
        throw new OppijanumeroServiceException("Unexpected response from ONR");
      }
    } catch (RetryException e) {
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
