/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.dto;

import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import java.util.UUID;

public record MahdollisuusDto(UUID id, MahdollisuusTyyppi tyyppi) {
  public MahdollisuusDto(UUID id, String tyyppi) {
    this(id, MahdollisuusTyyppi.valueOf(tyyppi));
  }
}
