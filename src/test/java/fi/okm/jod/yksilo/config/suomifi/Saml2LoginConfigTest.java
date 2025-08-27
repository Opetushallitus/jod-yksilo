/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.suomifi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import fi.okm.jod.yksilo.service.profiili.YksiloService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

class Saml2LoginConfigTest {
  @Test
  void shouldCreateRelyingPartyRepository() throws IOException {
    var props = new RelyingPartyProperties();
    props.setRegistrationId("test");
    props.setIdpMetadataUri("classpath:data/idp-metadata.xml");
    props.setCertificate(getContent("data/test.crt.pem"));
    props.setPrivateKey(getContent("data/test.key.pem"));

    var config = new Saml2LoginConfig(mock(YksiloService.class));
    var repo = assertDoesNotThrow(() -> config.relyingPartyRegistrationRepository(props));
    var registration = repo.findByRegistrationId("test");

    assertNotNull(registration);
    assertEquals(
        Saml2MessageBinding.REDIRECT,
        registration.getAssertingPartyMetadata().getSingleLogoutServiceBinding());
    assertEquals(
        Saml2MessageBinding.REDIRECT,
        registration.getAssertingPartyMetadata().getSingleSignOnServiceBinding());
  }

  private static @NotNull String getContent(String path) throws IOException {
    return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
  }
}
