/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fi.okm.jod.yksilo.IntegrationTest;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
    properties = {
      "jod.mpassid.enabled=true",
      "jod.mpassid.onr-oid-prefix=1.2.246.562.24.",
      "jod.mpassid.oidc.client-id=test",
      "jod.mpassid.oidc.client-secret=test",
      "jod.mpassid.oidc.issuer-uri=https://example.org",
      "jod.authentication.provider=mpassid",
    })
class MpassidOidcUserConverterTest extends IntegrationTest {

  @Autowired private MpassidOidcUserConverter userConverter;
  @Autowired private YksiloRepository yksilot;
  private OidcUserService service;

  private static final String OPPIJANUMERO_CLAIM_URI = Attribute.OPPIJANUMERO_CLAIM.getUri();
  private static final String VALID_ONR = "1.2.246.562.24.10000000003";

  @BeforeEach
  void setUp() {
    service = new OidcUserService();
    service.setOidcUserConverter(userConverter);
  }

  @Test
  void shouldCreateNewUserOnFirstLogin() {
    var request =
        createUserRequest(
            Map.of(
                OPPIJANUMERO_CLAIM_URI,
                VALID_ONR,
                "given_name",
                "Matti",
                "family_name",
                "Meikäläinen"));

    var principal = service.loadUser(request);

    assertNotNull(principal);
    var jodPrincipal = (JodOidcPrincipal) principal;
    var yksilo = yksilot.findById(jodPrincipal.getId()).orElseThrow();
    assertNotNull(yksilo);
    assertFalse(yksilo.getTervetuloapolku());
  }

  @Test
  void shouldReturnSameUserOnSubsequentLogin() {
    var claims =
        Map.<String, Object>of(
            OPPIJANUMERO_CLAIM_URI, VALID_ONR, "given_name", "Matti", "family_name", "Meikäläinen");
    var first = (JodOidcPrincipal) service.loadUser(createUserRequest(claims));
    var second = (JodOidcPrincipal) service.loadUser(createUserRequest(claims));

    assertEquals(first.getId(), second.getId());
  }

  @Test
  void shouldLinkToExistingAccountWithSameOppijanumero() {
    var qualifiedOnr = "ONR:" + VALID_ONR;
    var existingId = yksilot.upsertTunnistusData(null, qualifiedOnr, null, null);
    yksilot.save(new Yksilo(existingId));

    var request =
        createUserRequest(
            Map.of(
                OPPIJANUMERO_CLAIM_URI,
                VALID_ONR,
                "given_name",
                "Matti",
                "family_name",
                "Meikäläinen"));
    var principal = (JodOidcPrincipal) service.loadUser(request);

    assertEquals(existingId, principal.getId());
  }

  @Test
  void shouldFailWhenOppijanumeroClaimMissing() {
    var request = createUserRequest(Map.of("sub", "test"));

    assertThrows(OAuth2AuthenticationException.class, () -> service.loadUser(request));
  }

  @Test
  void shouldFailWhenOppijanumeroClaimBlank() {
    var request = createUserRequest(Map.of(OPPIJANUMERO_CLAIM_URI, "  "));

    assertThrows(OAuth2AuthenticationException.class, () -> service.loadUser(request));
  }

  @Test
  void shouldFailWhenOppijanumeroInvalid() {
    var request = createUserRequest(Map.of(OPPIJANUMERO_CLAIM_URI, "invalid-oid"));

    assertThrows(OAuth2AuthenticationException.class, () -> service.loadUser(request));
  }

  @Test
  void shouldUpdateNameOnSubsequentLogin() {
    var qualifiedOnr = "ONR:1.2.246.562.24.30000000009";
    var claims1 =
        Map.<String, Object>of(
            OPPIJANUMERO_CLAIM_URI,
            "1.2.246.562.24.30000000009",
            "given_name",
            "OldFirst",
            "family_name",
            "OldLast");
    var first = (JodOidcPrincipal) service.loadUser(createUserRequest(claims1));

    var claims2 =
        Map.<String, Object>of(
            OPPIJANUMERO_CLAIM_URI,
            "1.2.246.562.24.30000000009",
            "given_name",
            "NewFirst",
            "family_name",
            "NewLast");
    var second = (JodOidcPrincipal) service.loadUser(createUserRequest(claims2));

    assertEquals(first.getId(), second.getId());

    var data = yksilot.findTunnistusDataByOppijanumero(qualifiedOnr).orElseThrow();
    assertEquals("NewFirst", data.etunimi());
    assertEquals("NewLast", data.sukunimi());
  }

  private static OidcUserRequest createUserRequest(Map<String, Object> claims) {
    var clientRegistration =
        ClientRegistration.withRegistrationId("mpassid")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .clientId("test")
            .redirectUri("https://example.org/callback")
            .authorizationUri("https://example.org/auth")
            .tokenUri("https://example.org/token")
            .build();

    var now = Instant.now();
    var idToken = new OidcIdToken("token-value", now, now.plusSeconds(3600), claims);
    var accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, "access-token", now, now.plusSeconds(3600));

    return new OidcUserRequest(clientRegistration, accessToken, idToken);
  }
}
