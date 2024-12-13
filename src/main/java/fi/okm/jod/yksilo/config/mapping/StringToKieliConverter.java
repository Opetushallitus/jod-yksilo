/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.mapping;

import fi.okm.jod.yksilo.domain.Kieli;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToKieliConverter implements Converter<String, Kieli> {
  @Override
  public Kieli convert(String source) {
    try {
      return Kieli.valueOf(source.toUpperCase());
    } catch (IllegalArgumentException e) {
      // return FI as default value
      return Kieli.FI;
    }
  }
}
