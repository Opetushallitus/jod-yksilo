/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto;

import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.TyomahdollisuusAineisto;
import java.util.List;
import java.util.UUID;

public record MahdollisuusDto(
    UUID id,
    MahdollisuusTyyppi tyypi,
    String ammattiryhma,
    TyomahdollisuusAineisto aineisto,
    KoulutusmahdollisuusTyyppi koulutusTyyppi,
    List<String> maakunnat,
    Double kesto) {}
