/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto;

import fi.okm.jod.yksilo.domain.JakaumaTyyppi;
import fi.okm.jod.yksilo.domain.LocalizedString;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

public record TyomahdollisuusFullDto(
    UUID id,
    LocalizedString otsikko,
    LocalizedString tiivistelma,
    LocalizedString kuvaus,
    @Schema(propertyNames = JakaumaTyyppi.class, additionalPropertiesSchema = JakaumaDto.class)
        Map<JakaumaTyyppi, JakaumaDto> jakaumat) {}
