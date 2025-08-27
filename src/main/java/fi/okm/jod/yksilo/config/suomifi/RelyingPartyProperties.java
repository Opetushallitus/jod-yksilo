/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.suomifi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Pattern.Flag;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jod.saml2.relying-party")
@Getter
@Setter
@Validated
@ConditionalOnProperty(value = "jod.authentication.provider", havingValue = "suomifi")
public class RelyingPartyProperties {
  @NotBlank private String registrationId;
  @NotBlank private String idpMetadataUri;

  /** X.509 certificate in PKCS #8 PEM base64-encoded format. Used for signing and decryption. */
  @NotBlank
  @Pattern(
      regexp = "^-----BEGIN CERTIFICATE-.*",
      flags = {Flag.DOTALL})
  private String certificate;

  /** Private key in PKCS #8 PEM base64-encoded format. Used for signing and decryption. */
  @NotBlank
  @Pattern(
      regexp = "^-----BEGIN PRIVATE KEY-.*",
      flags = {Flag.DOTALL})
  private String privateKey;
}
