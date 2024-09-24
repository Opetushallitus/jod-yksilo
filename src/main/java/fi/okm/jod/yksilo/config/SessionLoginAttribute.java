/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import lombok.Getter;

@Getter
public enum SessionLoginAttribute {
  LANG,
  CALLBACK;

  private final String key;

  SessionLoginAttribute() {
    this.key = SessionLoginAttribute.class.getName() + "." + this.name();
  }
}
