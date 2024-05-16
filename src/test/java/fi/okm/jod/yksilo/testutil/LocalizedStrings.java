/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.testutil;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import java.util.Map;

public interface LocalizedStrings {
  static LocalizedString ls(String s) {
    return ls(Kieli.FI, s);
  }

  static LocalizedString ls(Kieli k, String s) {
    return new LocalizedString(Map.of(k, s));
  }

  static LocalizedString ls(Kieli k, String s, Kieli k2, String s2) {
    return new LocalizedString(Map.of(k, s, k2, s2));
  }
}
