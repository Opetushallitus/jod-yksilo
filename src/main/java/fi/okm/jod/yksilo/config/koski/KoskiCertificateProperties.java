/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.koski;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Pattern.Flag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "jod.koski.certificate")
@ConditionalOnProperty(name = "jod.koski.enabled", havingValue = "true")
@Getter(AccessLevel.PACKAGE)
@Setter(AccessLevel.PACKAGE)
@Validated
// Note: needs to be a mutable configuration bean for the refresh to work
class KoskiCertificateProperties {

  @NotBlank
  @Pattern(
      regexp = "^-----BEGIN CERTIFICATE-.*",
      flags = {Flag.DOTALL})
  private String fullChain;

  @NotBlank
  @Pattern(
      regexp = "^-----BEGIN PRIVATE KEY-.*",
      flags = {Flag.DOTALL})
  private String privateKey;
}
