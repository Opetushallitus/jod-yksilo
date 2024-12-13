/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.mapping;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class StringToSortDirectionConverter implements Converter<String, Sort.Direction> {
  @Override
  public Sort.Direction convert(String source) {
    try {
      return Sort.Direction.valueOf(source.toUpperCase());
    } catch (IllegalArgumentException e) {
      // return asc as default value
      return Sort.Direction.ASC;
    }
  }
}
