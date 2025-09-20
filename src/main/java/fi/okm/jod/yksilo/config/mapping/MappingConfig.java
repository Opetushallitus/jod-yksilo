/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import fi.okm.jod.yksilo.domain.LocalizedString;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MappingConfig {

  // limits for acceptable JSON input sizes
  public static final long MAX_DOC_LEN = 2 * 1024L * 1024L;
  public static final int MAX_STRING_LEN = 32 * 1024;
  public static final int MAX_NAME_LEN = 256;

  @Bean
  Jackson2ObjectMapperBuilderCustomizer customizer() {
    return builder ->
        builder
            .featuresToEnable(
                SerializationFeature.WRITE_ENUMS_USING_TO_STRING,
                MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .serializationInclusion(JsonInclude.Include.NON_ABSENT)
            .mixIn(LocalizedString.class, LocalizedStringMixin.class)
            .factory(
                JsonFactory.builder()
                    .streamReadConstraints(
                        StreamReadConstraints.builder()
                            .maxNameLength(MAX_NAME_LEN)
                            .maxStringLength(MAX_STRING_LEN)
                            .maxDocumentLength(MAX_DOC_LEN)
                            .build())
                    .build());
  }
}
