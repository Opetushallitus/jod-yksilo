/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.domain.LocalizedString;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class HibernateJsonMappingConfig {
  @Bean
  HibernatePropertiesCustomizer jsonFormatMapperCustomizer() {
    // NOTE: This version of Hibernate uses Jackson 2 ObjectMapper, therefore not injecting the
    // default (Jackson 3 one)
    var objectMapper =
        new ObjectMapper()
            .registerModules(ObjectMapper.findModules(this.getClass().getClassLoader()));
    objectMapper.addMixIn(LocalizedString.class, LocalizedStringMixin.class);

    return properties ->
        properties.put(
            AvailableSettings.JSON_FORMAT_MAPPER, new JacksonJsonFormatMapper(objectMapper));
  }
}
