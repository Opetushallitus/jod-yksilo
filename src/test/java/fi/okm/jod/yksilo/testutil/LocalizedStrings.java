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
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.util.Pair;

public interface LocalizedStrings {
  static LocalizedString ls(String s) {
    return ls(Kieli.FI, s);
  }

  static LocalizedString ls(Kieli k, String s) {
    return new LocalizedString(Map.of(k, s));
  }

  @SafeVarargs
  static LocalizedString ls(Pair<Kieli, String>... values) {
    return new LocalizedString(
        Arrays.stream(values).collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
  }

  static LocalizedString ls(Kieli k, String s, Kieli k2, String s2) {
    return new LocalizedString(Map.of(k, s, k2, s2));
  }
}
