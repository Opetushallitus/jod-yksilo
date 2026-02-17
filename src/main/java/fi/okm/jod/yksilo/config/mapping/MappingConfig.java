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
import fi.okm.jod.yksilo.domain.LocalizedString;
import java.util.List;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class MappingConfig {

  public static final long MAX_DOC_LEN = 2L * 1024 * 1024;
  public static final int MAX_STRING_LEN = 32 * 1024;
  public static final int MAX_NAME_LEN = 256;

  @Bean
  @Primary
  JsonMapper jsonMapper(List<JsonMapperBuilderCustomizer> customizers) {
    var constraints =
        StreamReadConstraints.builder()
            .maxDocumentLength(MAX_DOC_LEN)
            .maxStringLength(MAX_STRING_LEN)
            .maxNameLength(MAX_NAME_LEN)
            .build();

    var factory = JsonFactory.builder().streamReadConstraints(constraints).build();

    var builder =
        JsonMapper.builder(factory)
            .enable(EnumFeature.WRITE_ENUMS_USING_TO_STRING, EnumFeature.READ_ENUMS_USING_TO_STRING)
            .changeDefaultPropertyInclusion(
                incl -> incl.withValueInclusion(JsonInclude.Include.NON_ABSENT))
            .addMixIn(LocalizedString.class, LocalizedStringMixin.class);

    customizers.forEach(customizer -> customizer.customize(builder));
    return builder.build();
  }
}
