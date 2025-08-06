/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.externalapi;

import static fi.okm.jod.yksilo.externalapi.v1.ExternalApiV1Controller.EXT_API_V1_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;

import fi.okm.jod.yksilo.IntegrationTest;
import fi.okm.jod.yksilo.dto.SivuDto;
import fi.okm.jod.yksilo.externalapi.v1.dto.ExtProfiiliDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.util.UriComponentsBuilder;

/** Integration tests for external API */
class ExternalApiV1IntegrationTest extends IntegrationTest {

  @Autowired private TestRestTemplate testRestTemplate;

  @Sql(
      scripts = {"/data/add_10_yksiloa.sql"},
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(
      scripts = {"/data/cleanup.sql"},
      executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  @Execution(ExecutionMode.SAME_THREAD)
  @Test
  void getProfiilit200() {
    String profiilitUrlWithParams =
        UriComponentsBuilder.fromPath(EXT_API_V1_PATH + "/profiilit")
            .queryParam("sivu", "0")
            .queryParam("koko", "5")
            .encode()
            .toUriString();
    final ResponseEntity<SivuDto<ExtProfiiliDto>> response =
        this.testRestTemplate.exchange(
            profiilitUrlWithParams,
            HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            new ParameterizedTypeReference<>() {});
    assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    final SivuDto<ExtProfiiliDto> profiiliSivu = response.getBody();
    final ExtProfiiliDto testProfile = profiiliSivu.sisalto().getFirst();
    // check that collections contain members that add_10_yksiloa.sql added
    assertEquals(10, profiiliSivu.maara());
    assertEquals(5, profiiliSivu.sisalto().size());
    assertEquals(4, testProfile.osaamisKiinnostukset().size());
    assertEquals(5, testProfile.ammattiKiinnostukset().size());
    assertEquals(13, testProfile.suosikit().size());
    assertEquals(8, testProfile.paamaarat().size());
  }

  @Test
  void getProfiilit400TooBigPageSize() {
    String profiilitUrlWithParams =
        UriComponentsBuilder.fromPath(EXT_API_V1_PATH + "/profiilit")
            .queryParam("sivu", "0")
            .queryParam("koko", "5000")
            .encode()
            .toUriString();
    final ResponseEntity<SivuDto<ExtProfiiliDto>> response =
        this.testRestTemplate.exchange(
            profiilitUrlWithParams,
            HttpMethod.GET,
            new HttpEntity<>(new HttpHeaders()),
            new ParameterizedTypeReference<>() {});
    assertEquals(HttpStatusCode.valueOf(400), response.getStatusCode());
  }
}
