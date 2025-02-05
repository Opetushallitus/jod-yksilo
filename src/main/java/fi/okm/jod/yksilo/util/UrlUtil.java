/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.util;

import java.net.URI;
import java.net.URISyntaxException;

public class UrlUtil {

  private UrlUtil() {
    // Utility class.
  }

  public static String getRelativePath(String fullUrl) throws URISyntaxException {
    var uri = new URI(fullUrl);
    var path = uri.getPath();
    var query = uri.getQuery();

    return query != null ? path + "?" + query : path;
  }
}
