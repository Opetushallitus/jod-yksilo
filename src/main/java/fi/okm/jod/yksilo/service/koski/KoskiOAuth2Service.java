/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

import com.fasterxml.jackson.databind.JsonNode;
import fi.okm.jod.yksilo.config.koski.KoskiOAuth2Config;
import fi.okm.jod.yksilo.config.koski.KoskiRestClientConfig;
import fi.okm.jod.yksilo.domain.JodUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@ConditionalOnBean(KoskiOAuth2Config.class)
@Service
public class KoskiOAuth2Service {

  private final KoskiOAuth2Config koskiConfig;
  private final OAuth2AuthorizedClientRepository authorizedClientRepository;
  private final RestClient restClient;

  public KoskiOAuth2Service(
      KoskiOAuth2Config koskiConfig,
      OAuth2AuthorizedClientRepository authorizedClientRepository,
      @Qualifier(KoskiRestClientConfig.RESTCLIENT_ID) RestClient restClient) {
    this.koskiConfig = koskiConfig;
    this.authorizedClientRepository = authorizedClientRepository;
    this.restClient = restClient;
  }

  public String getRegistrationId() {
    return koskiConfig.getRegistrationId();
  }

  public OAuth2AuthorizedClient getAuthorizedClient(
      Authentication authentication, HttpServletRequest request) {
    return authorizedClientRepository.loadAuthorizedClient(
        koskiConfig.getRegistrationId(), authentication, request);
  }

  public void checkPersonIdMatches(JodUser jodUser, JsonNode jsonData) throws WrongPersonException {
    // if (true) return false; //Bypass for development purpose.
    var jodUserPersonId = jodUser.getPersonId();
    var oauth2PersonId = getPersonId(jsonData);
    if (!StringUtils.endsWithIgnoreCase(jodUserPersonId, oauth2PersonId)) {
      throw new WrongPersonException(jodUser.getId());
    }
  }

  private static String getPersonId(JsonNode jsonData) {
    return jsonData.get("henkilö").get("hetu").asText();
  }

  public void unauthorize(
      Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
    authorizedClientRepository.removeAuthorizedClient(
        getRegistrationId(), authentication, request, response);
  }

  public JsonNode fetchDataFromResourceServer(OAuth2AuthorizedClient auth2AuthorizedClient) {
    var accessToken = auth2AuthorizedClient.getAccessToken();
    if (Instant.now().isAfter(Objects.requireNonNull(accessToken.getExpiresAt()))) {
      throw new PermissionRequiredException("Token expired.");
    }
    try {
      return restClient
          .post()
          .uri(koskiConfig.getResourceServer())
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getTokenValue())
          .attributes(clientRegistrationId(koskiConfig.getRegistrationId()))
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(JsonNode.class);

    } catch (HttpClientErrorException e) {
      throw new ResourceServerException("Fail to get data from Koski resource server.", e);
    }
  }
}
