/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.login;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.okm.jod.yksilo.config.SessionConfig;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

class JodOidcPrincipalTest {

  @Test
  void shouldSerializeAndDeserialize() throws Exception {
    var id = UUID.randomUUID();
    var oppijanumero = "1.2.246.562.24.10000000003";
    var token = buildToken(oppijanumero, "Matti", "Meikäläinen");
    var principal = new JodOidcPrincipal(id, token);

    var validatorBuilder = BasicPolymorphicTypeValidator.builder().allowIfSubType(URL.class);
    var mapper =
        JsonMapper.builder()
            .addMixIn(JodOidcPrincipal.class, SessionConfig.JodOidcPrincipalMixin.class)
            .addModules(
                SecurityJacksonModules.getModules(
                    this.getClass().getClassLoader(), validatorBuilder))
            .build();

    var json = mapper.writeValueAsString(principal);
    assertNotNull(json);
    assertTrue(json.contains("JodOidcPrincipal"));

    var deserialized = mapper.readValue(json, JodOidcPrincipal.class);
    assertEquals(id, deserialized.getId());
    assertEquals(oppijanumero, deserialized.getOppijanumero());
    assertEquals("Matti", deserialized.givenName());
    assertEquals("Meikäläinen", deserialized.familyName());
    assertEquals("ONR:" + oppijanumero, deserialized.getQualifiedPersonId());
    assertEquals("1.2.246.562.24.10000000003", deserialized.getPersonId());
    assertThat(deserialized.getClaims()).containsAllEntriesOf(token.getClaims());
  }

  @Test
  void shouldReturnCorrectQualifiedPersonId() throws Exception {
    var token = buildToken("1.2.246.562.24.10000000003", "Test", "User");
    var principal = new JodOidcPrincipal(UUID.randomUUID(), token);

    assertEquals("ONR:1.2.246.562.24.10000000003", principal.getQualifiedPersonId());
    assertEquals("ONR:1.2.246.562.24.10000000003", principal.getName());
    assertEquals("1.2.246.562.24.10000000003", principal.getPersonId());
  }

  private static OidcIdToken buildToken(String oppijanumero, String givenName, String familyName)
      throws Exception {
    return new OidcIdToken(
        "token-value",
        Instant.now(),
        Instant.now().plusSeconds(3600),
        Map.of(
            "sub",
            "test-subject",
            "iss",
            URI.create("https://example.com/issuer").toURL(),
            Attribute.OPPIJANUMERO_CLAIM.getUri(),
            oppijanumero,
            "given_name",
            givenName,
            "family_name",
            familyName));
  }
}
