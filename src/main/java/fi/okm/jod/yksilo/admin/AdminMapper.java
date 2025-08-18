/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.admin;

import fi.okm.jod.yksilo.admin.dto.FullKoulutusData;
import fi.okm.jod.yksilo.admin.dto.KoulutusMahdollisuusResponseDto;
import fi.okm.jod.yksilo.admin.dto.KoulutusResponseDto;
import fi.okm.jod.yksilo.admin.dto.RawKoulutusDto;
import fi.okm.jod.yksilo.admin.dto.RawKoulutusMahdollisuusDto;
import java.util.List;

/** */
public class AdminMapper {
  private AdminMapper() {}

  public static List<KoulutusMahdollisuusResponseDto> toKoulutusMahdollisuusResponses(
      List<RawKoulutusMahdollisuusDto> koulutusMahdollisuudet,
      List<FullKoulutusData> koulutuksetDataSource) {
    return koulutusMahdollisuudet.stream()
        .map(km -> toKoulutusMahdollisuusResponse(km, koulutuksetDataSource))
        .toList();
  }

  public static KoulutusMahdollisuusResponseDto toKoulutusMahdollisuusResponse(
      RawKoulutusMahdollisuusDto koulutusMahdollisuus,
      List<FullKoulutusData> koulutuksetDataSource) {
    List<KoulutusResponseDto> koulutuksetResponse =
        koulutusMahdollisuus.koulutukset().stream()
            .map(k -> toKoulutusResponse(k, koulutuksetDataSource))
            .toList();
    return new KoulutusMahdollisuusResponseDto(
        koulutusMahdollisuus.id(),
        koulutusMahdollisuus.luomisaika(),
        koulutusMahdollisuus.otsikko(),
        koulutusMahdollisuus.tiivistelma(),
        koulutusMahdollisuus.kuvaus(),
        koulutusMahdollisuus.tyyppi(),
        koulutuksetResponse);
  }

  public static KoulutusResponseDto toKoulutusResponse(
      RawKoulutusDto rawKoulutusDto, List<FullKoulutusData> koulutuksetDataSource) {
    final FullKoulutusData koulutusDataSet =
        koulutuksetDataSource.stream()
            .filter(full -> full.getOid().equals(rawKoulutusDto.oid()))
            // only one koulutusdata should be found per oid
            .findFirst()
            .orElseThrow();
    return new KoulutusResponseDto(
        rawKoulutusDto.oid(),
        rawKoulutusDto.nimi(),
        koulutusDataSet.getKoulutusAlat(),
        koulutusDataSet.getKoulutuskoodit());
  }
}
