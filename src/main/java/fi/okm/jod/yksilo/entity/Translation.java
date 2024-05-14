/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.LocalizedString;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public interface Translation {

  boolean isEmpty();

  static <T extends Translation> void merge(
      LocalizedString source,
      Map<Kieli, T> target,
      Supplier<T> newTranslation,
      BiConsumer<T, String> setter) {
    Map<Kieli, String> values = source == null ? Map.of() : source.asMap();
    for (var kieli : Kieli.values()) {
      T e;
      String s;
      if ((s = values.get(kieli)) != null) {
        setter.accept(target.computeIfAbsent(kieli, k -> newTranslation.get()), s);
      } else if ((e = target.get(kieli)) != null) {
        setter.accept(e, null);
        if (e.isEmpty()) {
          target.remove(kieli);
        }
      }
    }
  }
}
