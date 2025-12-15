/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.domain;

import java.util.Locale;
import lombok.Getter;

@Getter
public enum Kieli {
  FI,
  SV,
  EN;

  public Locale toLocale() {
    return switch (this) {
      case FI -> Locale.forLanguageTag("fi-FI");
      case SV -> Locale.forLanguageTag("sv-FI"); // Swedish (Finland)
      case EN -> Locale.forLanguageTag("en");
    };
  }

  private final String koodi;

  Kieli() {
    this.koodi = name().toLowerCase().intern();
  }

  @Override
  public String toString() {
    return koodi;
  }
}
