/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.login;

import java.net.URI;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jod.mpassid")
@Getter
@Setter
public class MpassidProperties {
  private boolean enabled = false;
  private String onrOidPrefix = "1.2.246.562.24.";
  private boolean validateOnrChecksum = true;
  private Oidc oidc = new Oidc();

  @Getter
  @Setter
  public static class Oidc {
    private String clientId;
    private String clientSecret;
    private URI issuerUri;
    private URI authorizationUri;
    private URI tokenUri;
    private URI jwksUri;
    private URI logoutUri;
  }
}
