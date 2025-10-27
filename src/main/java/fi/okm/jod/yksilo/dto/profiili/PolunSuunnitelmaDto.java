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

import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import fi.okm.jod.yksilo.validation.Limits;
import fi.okm.jod.yksilo.validation.PrintableString;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Set;
import java.util.UUID;

public record PolunSuunnitelmaDto(
    @Null(groups = Add.class) @Schema(accessMode = READ_ONLY) UUID id,
    @NotEmpty @PrintableString @Size(max = 200) LocalizedString nimi,
    @Null(groups = Add.class) @Schema(accessMode = READ_ONLY) TavoiteYhteenvetoDto tavoite,
    @Null(groups = Add.class)
        @Schema(accessMode = READ_ONLY)
        @Size(max = Limits.VAIHE_PER_SUUNNITELMA)
        Set<@Valid @NotNull PolunVaiheDto> vaiheet,
    @Null(groups = Add.class) @Schema(accessMode = READ_ONLY) Set<@NotNull URI> osaamiset,
    @Null(groups = Add.class) @Schema(accessMode = READ_ONLY) Set<@NotNull URI> ignoredOsaamiset) {}
