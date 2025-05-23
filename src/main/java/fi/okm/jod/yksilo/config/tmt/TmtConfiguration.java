/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.tmt;

import java.net.URI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jod.tmt")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TmtConfiguration {
  private boolean enabled;
  private URI authorizationUrl;
  private URI apiUrl;
  private String kipaSubscriptionKey;
  private String tokenIssuer;
}
