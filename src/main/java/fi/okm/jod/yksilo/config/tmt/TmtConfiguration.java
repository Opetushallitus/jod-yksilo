/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.tmt;

import fi.okm.jod.yksilo.domain.Kieli;
import java.net.URI;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jod.tmt")
@Getter
@Setter
public class TmtConfiguration {
  private boolean enabled;
  private Api exportApi;
  private Api importApi;

  @Getter
  @Setter
  public static class Api {
    private URI apiUrl;

    /** Authorization endpoint base URL. */
    private URI authorizationUrl;

    /** Localized path for the authorization endpoint. */
    private Map<Kieli, String> authorizationPath;

    private URI tokenUrl;
    private String kipaSubscriptionKey;
    private String clientId;
    private String clientSecret;

    public String getAuthorizationPath(Kieli kieli) {
      return authorizationPath.get(kieli);
    }
  }
}
