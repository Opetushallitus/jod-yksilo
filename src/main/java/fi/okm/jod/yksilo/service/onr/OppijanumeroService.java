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
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@ConditionalOnProperty(name = "jod.onr.base-url")
public class OppijanumeroService {

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
      log.debug("Fetching oppijanumero from ONR");
      var checkResult =
          restClient
              .post()
              .uri("/henkilo/exists")
              .contentType(MediaType.APPLICATION_JSON)
              .body(new HenkiloExistenceCheckDto(etunimet, kutsumanimi, sukunimi, hetu))
              .retrieve()
              .body(ExistenceCheckResult.class);

      if (checkResult == null || checkResult.oid() == null) {
        log.info("Person not found in ONR");
        return Optional.empty();
      }

      var haeResult =
          restClient
              .get()
              .uri("/yleistunniste/hae/{oid}", checkResult.oid())
              .retrieve()
              .body(HaeResult.class);

      if (haeResult == null || haeResult.oppijanumero() == null) {
        throw new OppijanumeroServiceException(
            "Failed to fetch oppijanumero from ONR: empty response");
      }

      return Optional.of(OppijanumeroUtils.qualify(haeResult.oppijanumero(), oidPrefix));

    } catch (RestClientException e) {
      throw new OppijanumeroServiceException("Failed to fetch oppijanumero from ONR", e);
    } catch (IllegalArgumentException e) {
      throw new OppijanumeroServiceException("ONR returned invalid oppijanumero", e);
    }
  }

  record HenkiloExistenceCheckDto(
      String etunimet, String kutsumanimi, String sukunimi, String hetu) {}

  record ExistenceCheckResult(String oid) {}

  record HaeResult(String oid, String oppijanumero, boolean passivoitu) {}
  ;
}
