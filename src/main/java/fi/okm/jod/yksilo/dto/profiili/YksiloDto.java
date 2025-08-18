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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

public record YksiloDto(
    @Schema(accessMode = Schema.AccessMode.READ_ONLY) PersonIdentifierType tunnisteTyyppi,
    boolean tervetuloapolku,
    boolean lupaLuovuttaaTiedotUlkopuoliselle,
    boolean lupaArkistoida,
    boolean lupaKayttaaTekoalynKoulutukseen,
    Integer syntymavuosi,
    Sukupuoli sukupuoli,
    @Pattern(regexp = "[0-9]{3}") String kotikunta,
    @Pattern(regexp = "[a-z]{2}") String aidinkieli,
    Kieli valittuKieli) {}
