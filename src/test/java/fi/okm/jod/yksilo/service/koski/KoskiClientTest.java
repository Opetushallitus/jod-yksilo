/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import fi.okm.jod.yksilo.config.koski.KoskiOauth2Config;
import fi.okm.jod.yksilo.testutil.TestJodUser;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KoskiClientTest {

  private static final String RESOURCE_SERVER = "https://koski.test/omadata-oauth2/resource-server";
  private static final String FULL_SCOPE = "HENKILOTIEDOT_HETU OPISKELUOIKEUDET_KAIKKI_TIEDOT";
  private static final String ONE_STUDY_RIGHT =
      "[{\"oppilaitos\": {\"nimi\": {\"fi\": \"Opisto\"}}}]";

  private final TestJodUser user = new TestJodUser(UUID.randomUUID());
  private final RestClient.Builder restClient = RestClient.builder();
  private final MockRestServiceServer koski = MockRestServiceServer.bindTo(restClient).build();
  private final KoskiClient client =
      new KoskiClient(new KoskiOauth2Config(RESOURCE_SERVER), restClient.build());
  private final OAuth2AccessToken token =
      new OAuth2AccessToken(
          OAuth2AccessToken.TokenType.BEARER,
          "token",
          Instant.now(),
          Instant.now().plusSeconds(600));

  private void koskiAnswers(String hetu, String scope, String opiskeluoikeudet) {
    var body =
        """
        {"henkilö": {"hetu": "%s"}, "opiskeluoikeudet": %s, "tokenInfo": {"scope": "%s"}}
        """
            .formatted(hetu, opiskeluoikeudet, scope);
    koski
        .expect(requestTo(RESOURCE_SERVER))
        .andExpect(header("Authorization", "Bearer token"))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
  }

  @Test
  void shouldReturnStudyRecords_whenSharedWithFullScope() {
    koskiAnswers(user.getPersonId(), FULL_SCOPE, ONE_STUDY_RIGHT);

    var data = client.fetch(user, token);

    assertThat(data.path("opiskeluoikeudet").size()).isEqualTo(1);
  }

  @Test
  void shouldRequirePermission_whenTokenExpired() {
    var expired =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "token",
            Instant.now().minusSeconds(600),
            Instant.now().minusSeconds(1));

    assertThatThrownBy(() -> client.fetch(user, expired))
        .isInstanceOf(PermissionRequiredException.class)
        .hasMessage("Token expired.");
    koski.verify();
  }

  @Test
  void shouldRequirePermission_whenSharedWithNarrowerScope() {
    koskiAnswers(
        user.getPersonId(),
        "HENKILOTIEDOT_HETU OPISKELUOIKEUDET_SUORITETUT_TUTKINNOT",
        ONE_STUDY_RIGHT);

    assertThatThrownBy(() -> client.fetch(user, token))
        .isInstanceOf(PermissionRequiredException.class);
  }

  @Test
  void shouldThrowWrongPerson_whenDataIsOfAnotherPerson() {
    koskiAnswers("010101-123N", FULL_SCOPE, ONE_STUDY_RIGHT);

    assertThatThrownBy(() -> client.fetch(user, token)).isInstanceOf(WrongPersonException.class);
  }

  @Test
  void shouldThrowNoData_whenKoskiDoesNotKnowThePerson() {
    koski.expect(requestTo(RESOURCE_SERVER)).andRespond(withResourceNotFound());

    assertThatThrownBy(() -> client.fetch(user, token)).isInstanceOf(NoDataException.class);
  }

  @Test
  void shouldThrowResourceServerError_whenKoskiFails() {
    koski.expect(requestTo(RESOURCE_SERVER)).andRespond(withServerError());

    assertThatThrownBy(() -> client.fetch(user, token)).isInstanceOf(ResourceServerException.class);
  }

  @Test
  void shouldThrowResourceServerError_whenKoskiDoesNotAnswer() {
    koski
        .expect(requestTo(RESOURCE_SERVER))
        .andRespond(withException(new SocketTimeoutException("Read timed out")));

    assertThatThrownBy(() -> client.fetch(user, token)).isInstanceOf(ResourceServerException.class);
  }

  @Test
  void shouldThrowNoData_whenThereAreNoStudyRecords() {
    koskiAnswers(user.getPersonId(), FULL_SCOPE, "[]");

    assertThatThrownBy(() -> client.fetch(user, token)).isInstanceOf(NoDataException.class);
  }
}
