/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import fi.okm.jod.yksilo.config.koski.KoskiOauth2Config;
import fi.okm.jod.yksilo.config.koski.KoskiRestClientConfig;
import fi.okm.jod.yksilo.config.logging.LogMarker;
import fi.okm.jod.yksilo.domain.JodUser;
import io.micrometer.tracing.annotation.NewSpan;
import java.time.Instant;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/** Fetches the user's study records from the Koski omadata resource server. */
@Slf4j
@ConditionalOnBean(KoskiOauth2Config.class)
@Component
class KoskiClient {

  static final String REQUIRED_SCOPE = "OPISKELUOIKEUDET_KAIKKI_TIEDOT";

  private final KoskiOauth2Config koskiConfig;
  private final RestClient restClient;

  KoskiClient(
      KoskiOauth2Config koskiConfig,
      @Qualifier(KoskiRestClientConfig.RESTCLIENT_ID) RestClient restClient) {
    this.koskiConfig = koskiConfig;
    this.restClient = restClient;
  }

  /**
   * The user's Koski data, shared with the full scope and holding study rights.
   *
   * @throws NoDataException if Koski has no data for the user
   * @throws WrongPersonException if the data is of another person
   * @throws PermissionRequiredException if the token has expired or the data was shared without the
   *     full scope
   * @throws ResourceServerException if Koski cannot be reached or fails
   */
  @NewSpan("koski.fetch-data")
  public JsonNode fetch(JodUser user, OAuth2AccessToken accessToken) {
    var expiresAt = accessToken.getExpiresAt();
    if (expiresAt == null || Instant.now().isAfter(expiresAt)) {
      throw new PermissionRequiredException("Token expired.");
    }
    JsonNode data;
    try {
      data =
          restClient
              .post()
              .uri(koskiConfig.getResourceServer())
              .headers(headers -> headers.setBearerAuth(accessToken.getTokenValue()))
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(JsonNode.class);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
        log.debug("Resource server returned: {}", e.getMessage());
        throw new NoDataException(e.getMessage());
      }
      throw new ResourceServerException("Fail to get data from Koski resource server.", e);
    } catch (RestClientException e) {
      // A server error, a timeout, a failed connection or an unreadable answer.
      throw new ResourceServerException("Fail to get data from Koski resource server.", e);
    }
    checkPersonIdMatches(user, data);
    checkContent(data);
    return data;
  }

  private static void checkPersonIdMatches(JodUser jodUser, JsonNode jsonData) {
    var oauth2PersonId = getPersonId(jsonData);
    if (oauth2PersonId == null) {
      throw new NoDataException("User data not found.");
    }
    if (!StringUtils.endsWithIgnoreCase(jodUser.getPersonId(), oauth2PersonId)) {
      log.atWarn()
          .addMarker(LogMarker.AUDIT)
          .log("User {} tried to access another person's Koski data.", jodUser.getId());
      throw new WrongPersonException(jodUser.getId());
    }
  }

  /** The import needs the full scope; a consent without it does not give enough to import. */
  private static void checkContent(JsonNode jsonData) {
    var scopes = Set.of(jsonData.path("tokenInfo").path("scope").stringValue("").split(" "));
    if (!scopes.contains(REQUIRED_SCOPE)) {
      throw new PermissionRequiredException(
          "Koski data was shared without scope " + REQUIRED_SCOPE);
    }
    var opiskeluoikeudet = jsonData.path("opiskeluoikeudet");
    if (!opiskeluoikeudet.isArray() || opiskeluoikeudet.isEmpty()) {
      throw new NoDataException("No opiskeluoikeudet in Koski data.");
    }
  }

  private static String getPersonId(JsonNode jsonData) {
    if (jsonData == null) {
      return null;
    }
    var hetu = jsonData.path("henkilö").path("hetu");
    return hetu.isMissingNode() ? null : hetu.asString();
  }
}
