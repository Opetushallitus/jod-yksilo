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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Verifies that the {@code ?prefix=} paths used in {@code application-cloud.yml} bind into {@link
 * RelyingPartyProperties}.
 */
class RelyingPartyPropertiesBindingTest {

  @Test
  void shouldBindBothCredentialsFromSeparatePropertySources() {
    var properties =
        bind(flatSecret("credential", "current"), flatSecret("next-credential", "pending"));

    assertEquals("-----BEGIN CERTIFICATE-current", properties.getCredential().getCertificate());
    assertEquals("-----BEGIN PRIVATE KEY-current", properties.getCredential().getPrivateKey());
    assertEquals("-----BEGIN CERTIFICATE-pending", properties.getNextCredential().getCertificate());
    assertEquals("-----BEGIN PRIVATE KEY-pending", properties.getNextCredential().getPrivateKey());
  }

  @Test
  void shouldLeaveNextCredentialUnsetOutsideRollover() {
    var properties = bind(flatSecret("credential", "current"));

    assertNotNull(properties.getCredential());
    assertNull(properties.getNextCredential());
  }

  /**
   * Demonstrates the limitation this design works around: indexed entries contributed by two
   * property sources do not merge, only the higher priority one survives.
   */
  @Test
  void shouldNotMergeIndexedEntriesAcrossPropertySources() {
    var propertySources = new MutablePropertySources();
    propertySources.addLast(flatSecret("credentials[0]", "current"));
    propertySources.addLast(flatSecret("credentials[1]", "pending"));

    var bound =
        new Binder(ConfigurationPropertySources.from(propertySources))
            .bind(
                "jod.saml2.relying-party.credentials",
                Bindable.listOf(RelyingPartyProperties.Credential.class))
            .get();

    assertEquals(1, bound.size());
  }

  /**
   * Mimics a {@code SecretsManagerPropertySource}: the secret's JSON keys, in camelCase, flattened
   * under the prefix given in the {@code spring.config.import} location.
   */
  private static MapPropertySource flatSecret(String property, String value) {
    var prefix = "jod.saml2.relying-party." + property + ".";
    Map<String, Object> source = new LinkedHashMap<>();
    source.put(prefix + "certificate", "-----BEGIN CERTIFICATE-" + value);
    source.put(prefix + "privateKey", "-----BEGIN PRIVATE KEY-" + value);
    return new MapPropertySource("aws-secretsmanager:" + property, source);
  }

  private static RelyingPartyProperties bind(MapPropertySource... sources) {
    var propertySources = new MutablePropertySources();
    for (var source : sources) {
      propertySources.addLast(source);
    }
    return new Binder(ConfigurationPropertySources.from(propertySources))
        .bind("jod.saml2.relying-party", RelyingPartyProperties.class)
        .get();
  }
}
