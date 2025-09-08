/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
import fi.okm.jod.yksilo.dto.profiili.export.KiinnostuksetExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.KoulutusExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.KoulutusKokonaisuusExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.MuuOsaaminenExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.PaamaaraExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.PatevyysExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.PolunSuunnitelmaExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.PolunVaiheExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.ToimenkuvaExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.ToimintoExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.TyopaikkaExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.YksiloExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.YksilonSuosikkiExportDto;
import fi.okm.jod.yksilo.entity.Ammatti;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Paamaara;
import fi.okm.jod.yksilo.entity.Patevyys;
import fi.okm.jod.yksilo.entity.PolunSuunnitelma;
import fi.okm.jod.yksilo.entity.PolunVaihe;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonSuosikki;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ExportMapper {

  private ExportMapper() {}

  public static YksiloExportDto mapYksilo(Yksilo entity) {
    return entity == null
        ? null
        : new YksiloExportDto(
            entity.getId(),
            entity.getTervetuloapolku(),
            entity.getLupaLuovuttaaTiedotUlkopuoliselle(),
            entity.getLupaArkistoida(),
            entity.getLupaKayttaaTekoalynKoulutukseen(),
            entity.getSyntymavuosi(),
            entity.getSukupuoli(),
            entity.getKotikunta(),
            entity.getAidinkieli(),
            entity.getValittuKieli(),
            entity.getTyopaikat().stream()
                .map(ExportMapper::mapTyopaikka)
                .collect(Collectors.toSet()),
            entity.getKoulutusKokonaisuudet().stream()
                .map(ExportMapper::mapKoulutusKokonaisuus)
                .collect(Collectors.toSet()),
            entity.getToiminnot().stream()
                .map(ExportMapper::mapToiminto)
                .collect(Collectors.toSet()),
            new MuuOsaaminenExportDto(
                entity.getMuuOsaaminenVapaateksti(),
                entity.getOsaamiset().stream()
                    .filter(yo -> OsaamisenLahdeTyyppi.MUU_OSAAMINEN.equals(yo.getLahdeTyyppi()))
                    .map(yo -> yo.getOsaaminen().getUri())
                    .collect(Collectors.toSet())),
            new KiinnostuksetExportDto(
                entity.getOsaamisKiinnostuksetVapaateksti(),
                entity.getOsaamisKiinnostukset().stream()
                    .map(Osaaminen::getUri)
                    .collect(Collectors.toSet()),
                entity.getAmmattiKiinnostukset().stream()
                    .map(Ammatti::getUri)
                    .collect(Collectors.toSet())),
            entity.getSuosikit().stream()
                .map(ExportMapper::mapYksilonSuosikki)
                .collect(Collectors.toSet()),
            entity.getPaamaarat().stream()
                .map(ExportMapper::mapPaamaara)
                .collect(Collectors.toSet()));
  }

  public static TyopaikkaExportDto mapTyopaikka(Tyopaikka entity) {
    return entity == null
        ? null
        : new TyopaikkaExportDto(
            entity.getId(),
            entity.getNimi(),
            entity.getToimenkuvat().stream()
                .map(ExportMapper::mapToimenkuva)
                .collect(Collectors.toSet()));
  }

  public static ToimenkuvaExportDto mapToimenkuva(Toimenkuva entity) {
    return entity == null
        ? null
        : new ToimenkuvaExportDto(
            entity.getId(),
            entity.getAlkuPvm(),
            entity.getLoppuPvm(),
            entity.getNimi(),
            entity.getKuvaus(),
            entity.getOsaamiset().stream()
                .map(yksilonOsaaminen -> yksilonOsaaminen.getOsaaminen().getUri())
                .collect(Collectors.toSet()));
  }

  public static KoulutusKokonaisuusExportDto mapKoulutusKokonaisuus(KoulutusKokonaisuus entity) {
    return entity == null
        ? null
        : new KoulutusKokonaisuusExportDto(
            entity.getId(),
            entity.getNimi(),
            entity.getKoulutukset().stream()
                .map(ExportMapper::mapKoulutus)
                .collect(Collectors.toSet()));
  }

  public static KoulutusExportDto mapKoulutus(Koulutus entity) {
    return entity == null
        ? null
        : new KoulutusExportDto(
            entity.getId(),
            entity.getAlkuPvm(),
            entity.getLoppuPvm(),
            entity.getNimi(),
            entity.getKuvaus(),
            entity.getOsaamiset().stream()
                .map(yksilonOsaaminen -> yksilonOsaaminen.getOsaaminen().getUri())
                .collect(Collectors.toSet()),
            entity.getOsaamisenTunnistusStatus(),
            entity.getOsasuoritukset());
  }

  public static ToimintoExportDto mapToiminto(Toiminto entity) {
    return entity == null
        ? null
        : new ToimintoExportDto(
            entity.getId(),
            entity.getNimi(),
            entity.getPatevyydet().stream()
                .map(ExportMapper::mapPatevyys)
                .collect(Collectors.toSet()));
  }

  public static PatevyysExportDto mapPatevyys(Patevyys entity) {
    return entity == null
        ? null
        : new PatevyysExportDto(
            entity.getId(),
            entity.getAlkuPvm(),
            entity.getLoppuPvm(),
            entity.getNimi(),
            entity.getKuvaus(),
            entity.getOsaamiset().stream()
                .map(yksilonOsaaminen -> yksilonOsaaminen.getOsaaminen().getUri())
                .collect(Collectors.toSet()));
  }

  public static YksilonSuosikkiExportDto mapYksilonSuosikki(YksilonSuosikki entity) {
    return entity == null
        ? null
        : new YksilonSuosikkiExportDto(
            entity.getId(),
            entity.getLuotu(),
            getTyomahdollisuusId(entity.getTyomahdollisuus()),
            getKoulutusmahdollisuusId(entity.getKoulutusmahdollisuus()),
            entity.getTyyppi());
  }

  private static UUID getTyomahdollisuusId(Tyomahdollisuus tyomahdollisuus) {
    return tyomahdollisuus != null ? tyomahdollisuus.getId() : null;
  }

  private static UUID getKoulutusmahdollisuusId(Koulutusmahdollisuus koulutusmahdollisuus) {
    return koulutusmahdollisuus != null ? koulutusmahdollisuus.getId() : null;
  }

  public static PaamaaraExportDto mapPaamaara(Paamaara entity) {
    return entity == null
        ? null
        : new PaamaaraExportDto(
            entity.getId(),
            entity.getLuotu(),
            entity.getTyyppi(),
            getTyomahdollisuusId(entity.getTyomahdollisuus()),
            getKoulutusmahdollisuusId(entity.getKoulutusmahdollisuus()),
            entity.getSuunnitelmat().stream().map(ExportMapper::mapPolunSuunnitelma).toList(),
            entity.getTavoite());
  }

  public static PolunSuunnitelmaExportDto mapPolunSuunnitelma(PolunSuunnitelma entity) {
    return entity == null
        ? null
        : new PolunSuunnitelmaExportDto(
            entity.getId(),
            entity.getNimi(),
            entity.getVaiheet().stream().map(ExportMapper::mapPolunVaihe).toList(),
            entity.getOsaamiset().stream().map(Osaaminen::getUri).collect(Collectors.toSet()),
            entity.getIgnoredOsaamiset().stream()
                .map(Osaaminen::getUri)
                .collect(Collectors.toSet()));
  }

  public static PolunVaiheExportDto mapPolunVaihe(PolunVaihe entity) {
    return entity == null
        ? null
        : new PolunVaiheExportDto(
            entity.getId(),
            entity.getLahde(),
            entity.getTyyppi(),
            entity.getKoulutusmahdollisuus() != null
                ? entity.getKoulutusmahdollisuus().getId()
                : null,
            entity.getNimi(),
            entity.getKuvaus(),
            entity.getLinkit(),
            entity.getAlkuPvm(),
            entity.getLoppuPvm(),
            entity.getOsaamiset().stream().map(Osaaminen::getUri).collect(Collectors.toSet()),
            entity.isValmis());
  }
}
