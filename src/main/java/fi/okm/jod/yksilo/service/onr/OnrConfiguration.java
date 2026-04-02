/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.onr;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConditionalOnProperty(name = "jod.onr.base-url")
@ConfigurationProperties(prefix = "jod.onr")
@Slf4j
public class OnrConfiguration {
  @NotBlank private final String baseUrl;
  @NotBlank private final String oidPrefix;
  @NotNull @Valid private final Oauth2Config oauth2;

  public OnrConfiguration(String baseUrl, String oidPrefix, Oauth2Config oauth2) {
    this.baseUrl = baseUrl;
    this.oidPrefix = oidPrefix;
    this.oauth2 = oauth2;
  }

  public record Oauth2Config(
      @NotBlank String clientId, @NotBlank String clientSecret, @NotBlank String tokenUri) {}
}
