/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.suomifi;

import java.net.URI;
import lombok.Getter;

@Getter
public enum Loa {
  LOA3("http://ftn.ficora.fi/2017/loa3", 3),
  EIDAS_HIGH("http://eidas.europa.eu/LoA/high", 3),
  LOA2("http://ftn.ficora.fi/2017/loa2", 2),
  EIDAS_SUBSTANTIAL("http://eidas.europa.eu/LoA/substantial", 2),
  TEST("urn:oid:1.2.246.517.3002.110.999", 0);

  private final URI uri;
  private final int level;

  Loa(String uri, int level) {
    this.uri = URI.create(uri);
    this.level = level;
  }

  public static Loa fromUri(URI uri) {
    for (Loa loa : values()) {
      if (loa.uri.equals(uri)) {
        return loa;
      }
    }
    throw new IllegalArgumentException("Unknown LoA");
  }
}
