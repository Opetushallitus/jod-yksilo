/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import static org.assertj.core.api.Assertions.assertThat;

import fi.okm.jod.yksilo.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

class SessionConfigTest extends IntegrationTest {

  @Autowired private SessionRepository<?> sessions;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void shouldPersistSessionInPostgres() {
    verifySessionPersistence(sessions);
  }

  private <S extends Session> void verifySessionPersistence(SessionRepository<S> sessions) {
    var session = sessions.createSession();
    session.setAttribute("language", "fi");
    sessions.save(session);

    String language = sessions.findById(session.getId()).getAttribute("language");
    assertThat(language).isEqualTo("fi");
    var stored =
        jdbc.queryForObject(
            "SELECT ATTRIBUTE_BYTES FROM yksilo.SPRING_SESSION_ATTRIBUTES"
                + " WHERE SESSION_PRIMARY_ID = (SELECT PRIMARY_ID FROM yksilo.SPRING_SESSION"
                + " WHERE SESSION_ID = ?) AND ATTRIBUTE_NAME = ?",
            byte[].class,
            session.getId(),
            "language");
    assertThat(new String(stored, StandardCharsets.UTF_8)).contains("fi");

    sessions.deleteById(session.getId());
    assertThat(sessions.findById(session.getId())).isNull();
  }

  @ParameterizedTest
  @MethodSource("scopes")
  void shouldRoundTripOauth2AuthorizedClientWithScopes(List<String> scopes) {
    var registrationBuilder =
        ClientRegistration.withRegistrationId("tmt-vienti")
            .clientId("client-id")
            .clientSecret("client-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/oauth2/response/{registrationId}")
            .authorizationUri("https://example.com/authorize")
            .tokenUri("https://example.com/token")
            .clientName("tmt-vienti")
            .scope(scopes);
    var registration = registrationBuilder.build();
    var accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-token",
            Instant.now(),
            Instant.now().plusSeconds(300));
    Map<String, Object> sessionAttribute = new HashMap<>();
    sessionAttribute.put(
        "tmt-vienti", new OAuth2AuthorizedClient(registration, "principal", accessToken));

    var deserialized = reloadSessionAttribute(sessions, sessionAttribute);

    assertThat(deserialized).isInstanceOf(Map.class);
    assertThat(((Map<?, ?>) deserialized).get("tmt-vienti"))
        .isInstanceOfSatisfying(
            OAuth2AuthorizedClient.class,
            client -> {
              assertThat(client.getClientRegistration().getScopes())
                  .containsExactlyElementsOf(scopes);
              assertThat(
                      client
                          .getClientRegistration()
                          .getProviderDetails()
                          .getConfigurationMetadata())
                  .isEmpty();
              assertThat(client.getAccessToken().getTokenValue()).isEqualTo("access-token");
            });
  }

  private static Stream<List<String>> scopes() {
    return Stream.of(List.of(), List.of("openid", "profile"));
  }

  private <S extends Session> Object reloadSessionAttribute(
      SessionRepository<S> sessions, Object value) {
    var session = sessions.createSession();
    session.setAttribute("client", value);
    sessions.save(session);
    var reloaded = sessions.findById(session.getId()).getAttribute("client");
    sessions.deleteById(session.getId());
    return reloaded;
  }
}
