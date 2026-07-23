/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto.profiili;

import fi.okm.jod.yksilo.domain.KoskiTehtavaTila;
import java.util.List;
import java.util.UUID;

public record KoskiTehtavaDto(UUID id, KoskiTehtavaTila tila, Tulos tulos) {
  public record Tulos(List<KoulutusKokonaisuusDto> koulutuskokonaisuudet) {}
}
