/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.domain;

import java.util.Optional;
import lombok.Getter;

@Getter
public enum Kieli {
  FI,
  SV,
  EN;

  private final String koodi;

  Kieli() {
    this.koodi = name().toLowerCase().intern();
  }

  @Override
  public String toString() {
    return koodi;
  }

  public static Optional<Kieli> fromKoodi(String koodi) {
    for (Kieli value : values()) {
      if (value.koodi.equalsIgnoreCase(koodi)) {
        return Optional.of(value);
      }
    }
    return Optional.empty();
  }
}
