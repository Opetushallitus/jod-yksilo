/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;

import fi.okm.jod.yksilo.domain.SuosikkiTyyppi;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Null;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.With;

@With
public record JakolinkkiUpdateDto(
    @Null(groups = Add.class) @Schema(accessMode = READ_ONLY) UUID id,
    @Null(groups = Add.class) @Schema(accessMode = READ_ONLY) UUID ulkoinenId,
    String nimi,
    Instant voimassaAsti,
    String muistiinpano,
    boolean nimiJaettu,
    boolean emailJaettu,
    boolean kotikuntaJaettu,
    boolean syntymavuosiJaettu,
    boolean muuOsaaminenJaettu,
    boolean kiinnostuksetJaettu,
    Set<UUID> jaetutTyopaikat,
    Set<UUID> jaetutKoulutukset,
    Set<UUID> jaetutToiminnot,
    Set<SuosikkiTyyppi> jaetutSuosikit,
    Set<UUID> jaetutTavoitteet) {}
