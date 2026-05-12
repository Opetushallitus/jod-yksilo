/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

/**
 * A no-op implementation of {@link OAuth2AuthorizedClientRepository} that discards all authorized
 * client state. Used when OAuth2/OIDC is only needed for authentication (login) and the
 * access/refresh tokens are not needed after authentication.
 */
final class NoopOauth2AuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

  @Override
  public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
      String clientRegistrationId, Authentication principal, HttpServletRequest request) {
    return null;
  }

  @Override
  public void saveAuthorizedClient(
      OAuth2AuthorizedClient authorizedClient,
      Authentication principal,
      HttpServletRequest request,
      HttpServletResponse response) {
    // no-op
  }

  @Override
  public void removeAuthorizedClient(
      String clientRegistrationId,
      Authentication principal,
      HttpServletRequest request,
      HttpServletResponse response) {
    // no-op
  }
}
