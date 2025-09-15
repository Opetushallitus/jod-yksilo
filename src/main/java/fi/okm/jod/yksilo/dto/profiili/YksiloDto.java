/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.PersonIdentifierType;
import fi.okm.jod.yksilo.domain.Sukupuoli;
import fi.okm.jod.yksilo.validation.LanguageCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record YksiloDto(
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) PersonIdentifierType tunnisteTyyppi,
    boolean tervetuloapolku,
    boolean lupaLuovuttaaTiedotUlkopuoliselle,
    boolean lupaKayttaaTekoalynKoulutukseen,
    Integer syntymavuosi,
    Sukupuoli sukupuoli,
    @Pattern(regexp = "[0-9]{3}") String kotikunta,
    @LanguageCode String aidinkieli,
    Kieli valittuKieli,
    @Email @Size(max = 254) String email) {}
