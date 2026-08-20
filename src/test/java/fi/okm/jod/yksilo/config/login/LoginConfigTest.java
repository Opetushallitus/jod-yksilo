/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.login;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import fi.okm.jod.yksilo.service.profiili.YksiloService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

class LoginConfigTest {
  @Test
  void shouldCreateRelyingPartyRepository() throws IOException {
    var props = baseProps();

    var config = new LoginConfig(mock(YksiloService.class));
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

  @Test
  void shouldRejectWrongCertificate() throws IOException {
    var props = baseProps();
    // SP cert was not used to sign the IDP metadata
    props.setIdpMetadataSigningCaCertificate(getContent("data/test.crt.pem"));

    var config = new LoginConfig(mock(YksiloService.class));
    assertThrows(
        IllegalStateException.class, () -> config.relyingPartyRegistrationRepository(props));
  }

  @Test
  void shouldRejectMetadataWithTamperedSignedContent() throws IOException {
    var tamperedMetadata = Files.createTempFile("tampered-idp-metadata", ".xml");
    try {
      var metadata =
          getContent("data/idp-metadata.xml")
              .replace("https://example.org/idp1", "https://attacker.example/idp1");
      Files.writeString(tamperedMetadata, metadata, StandardCharsets.UTF_8);

      var props = baseProps();
      props.setIdpMetadataUri(tamperedMetadata.toUri().toString());

      var config = new LoginConfig(mock(YksiloService.class));
      var exception =
          assertThrows(
              IllegalStateException.class, () -> config.relyingPartyRegistrationRepository(props));
      assertEquals("IDP metadata signature validation failed", exception.getMessage());
    } finally {
      Files.deleteIfExists(tamperedMetadata);
    }
  }

  private RelyingPartyProperties baseProps() throws IOException {
    var props = new RelyingPartyProperties();
    props.setRegistrationId("test");
    props.setIdpMetadataUri("classpath:data/idp-metadata.xml");
    props.setCertificate(getContent("data/test.crt.pem"));
    props.setPrivateKey(getContent("data/test.key.pem"));
    props.setIdpMetadataSigningCaCertificate(getContent("data/idp.crt.pem"));
    return props;
  }

  private static @NotNull String getContent(String path) throws IOException {
    return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
  }
}
