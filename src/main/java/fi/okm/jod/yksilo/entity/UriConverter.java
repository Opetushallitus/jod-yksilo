/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter(autoApply = true)
public class UriConverter implements AttributeConverter<URI, String> {
  @Override
  public String convertToDatabaseColumn(URI attribute) {
    // Serialization: URI object to database string.
    if (attribute == null) {
      return null;
    }
    return attribute.toString();
  }

  @Override
  public URI convertToEntityAttribute(String dbData) {
    // Deserialization: database string to URI object.
    if (dbData == null) {
      return null;
    }
    return URI.create(dbData);
  }
}
