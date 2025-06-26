/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.externalapi.v1.dto;

import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;

public class ExtTyomahdollisuusMapper {
  private ExtTyomahdollisuusMapper() {}

  public static ExtTyoMahdollisuusDto toTyoMahdollisuusDto(final Tyomahdollisuus tyomahdollisuus) {
    return new ExtTyoMahdollisuusDto(
        tyomahdollisuus.getId(),
        tyomahdollisuus.getOtsikko(),
        tyomahdollisuus.getKuvaus(),
        tyomahdollisuus.getTiivistelma(),
        tyomahdollisuus.getAmmattiryhma(),
        tyomahdollisuus.isAktiivinen());
  }
}
