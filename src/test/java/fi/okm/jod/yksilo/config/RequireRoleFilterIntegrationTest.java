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
import fi.okm.jod.yksilo.errorhandler.ErrorInfo;
import fi.okm.jod.yksilo.errorhandler.ErrorInfo.ErrorCode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.ObjectMapper;

/**
 * The OAuth2 client endpoints check the role before Spring Security's exception handling, so the
 * error handling is verified against a real servlet container. TMT is used because its filter chain
 * can be started without external certificates; Koski uses the same filter.
 */
@TestPropertySource(
    properties = {
      "jod.tmt.enabled=true",
      "spring.config.import=classpath:/oauth2-tmt.yml",
      "jod.tmt.import-api.client-id=jod",
      "jod.tmt.import-api.client-secret=secret",
      "jod.tmt.import-api.token-url=http://localhost:1/token",
      "jod.tmt.import-api.authorization-url=http://localhost:1/authorize",
      "jod.tmt.export-api.client-id=jod",
      "jod.tmt.export-api.client-secret=secret",
      "jod.tmt.export-api.token-url=http://localhost:1/token",
      "jod.tmt.export-api.authorization-url=http://localhost:1/authorize"
    })
class RequireRoleFilterIntegrationTest extends IntegrationTest {

  private static final String AUTHORIZATION_PATH =
      "/yksilo/oauth2/authorization/tmt-haku?callback=/yksilo/fi";

  @LocalServerPort private int port;
  @Autowired private ObjectMapper mapper;

  private final HttpClient client = HttpClient.newHttpClient();

  @Test
  void shouldRespondWithAuthenticationFailure_whenUserIsNotLoggedIn() throws Exception {
    var response = send(HttpRequest.newBuilder(uri(AUTHORIZATION_PATH)));

    assertThat(response.statusCode()).isEqualTo(403);
    assertThat(mapper.readValue(response.body(), ErrorInfo.class).errorCode())
        .isEqualTo(ErrorCode.AUTHENTICATION_FAILURE);
  }

  @Test
  void shouldRedirectBrowserToFrontPage_whenUserIsNotLoggedIn() throws Exception {
    var response =
        send(HttpRequest.newBuilder(uri(AUTHORIZATION_PATH)).header("Sec-Fetch-Mode", "navigate"));

    assertThat(response.statusCode()).isEqualTo(303);
    assertThat(response.headers().firstValue("Location"))
        .hasValue("/yksilo/?error=AUTHENTICATION_FAILURE");
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }

  private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
    return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
  }
}
