/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.mapping;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import fi.okm.jod.yksilo.domain.LocalizedString;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MappingConfig {
  @Bean
  Jackson2ObjectMapperBuilderCustomizer customizer() {
    return builder ->
        builder
            .featuresToEnable(
                SerializationFeature.WRITE_ENUMS_USING_TO_STRING,
                MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .mixIn(LocalizedString.class, LocalizedStringMixin.class);
  }
}
