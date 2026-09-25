/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.login;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

  /**
   * Credential used for signing outgoing messages and decrypting incoming ones.
   *
   * <p>The application only deals with PEM encoded material, so this can come from any property
   * source (local YAML, environment variables, AWS Secrets Manager, ...).
   */
  @NotNull @Valid private Credential credential;

  /**
   * Optional additional credential used for decryption only, to support key rollover.
   *
   * <p>During a rollover the identity provider may still be encrypting to the previous key, or may
   * already have been switched to the upcoming one, depending on how far the (out of band) metadata
   * update has progressed. Configuring it here keeps logins working across that window. It is
   * deliberately never used for signing: only {@link #credential} signs, so promoting a key is a
   * matter of moving it into that property.
   */
  @Valid private Credential nextCredential;

  @Getter
  @Setter
  public static class Credential {
    /** X.509 certificate in PKCS #8 PEM base64-encoded format. */
    @NotBlank
    @Pattern(
        regexp = "^-----BEGIN CERTIFICATE-.*",
        flags = {Flag.DOTALL})
    private String certificate;

    /** Private key in PKCS #8 PEM base64-encoded format. */
    @NotBlank
    @Pattern(
        regexp = "^-----BEGIN PRIVATE KEY-.*",
        flags = {Flag.DOTALL})
    private String privateKey;
  }

  /**
   * CA X.509 certificate in PEM format. Used to verify that the signing certificate embedded in the
   * IDP metadata XML signature (ds:Signature/ds:KeyInfo) is issued by this trusted CA.
   */
  @NotBlank
  @Pattern(
      regexp = "^-----BEGIN CERTIFICATE-.*",
      flags = {Flag.DOTALL})
  private String idpMetadataSigningCaCertificate;
}
