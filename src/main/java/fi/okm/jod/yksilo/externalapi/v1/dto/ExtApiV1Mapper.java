/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.externalapi.v1.dto;

import fi.okm.jod.yksilo.entity.Ammatti;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Paamaara;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.entity.YksilonSuosikki;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** */
public class ExtApiV1Mapper {
  private ExtApiV1Mapper() {}

  public static ExtTyoMahdollisuusDto toTyoMahdollisuusDto(final Tyomahdollisuus tyomahdollisuus) {
    return new ExtTyoMahdollisuusDto(
        tyomahdollisuus.getId(),
        tyomahdollisuus.getOtsikko(),
        tyomahdollisuus.getKuvaus(),
        tyomahdollisuus.getTiivistelma(),
        tyomahdollisuus.getAmmattiryhma(),
        tyomahdollisuus.isAktiivinen());
  }

  public static ExtKoulutusMahdollisuusDto toKoulutusMahdollisuusDto(
      Koulutusmahdollisuus koulutusmahdollisuus) {
    return new ExtKoulutusMahdollisuusDto(
        koulutusmahdollisuus.getId(),
        koulutusmahdollisuus.getOtsikko(),
        koulutusmahdollisuus.getTiivistelma(),
        koulutusmahdollisuus.getKuvaus(),
        koulutusmahdollisuus.getKesto(),
        koulutusmahdollisuus.isAktiivinen());
  }

  public static byte[] uuidToBytes(UUID uuid) {
    ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
    buffer.putLong(uuid.getMostSignificantBits());
    buffer.putLong(uuid.getLeastSignificantBits());
    return buffer.array();
  }

  public static ExtProfiiliDto toProfiiliDto(Yksilo yksilo) {
    String id = hashedId(yksilo.getId());
    Set<ExtYksilonOsaaminenDto> yksilonOsaamiset =
        yksilo.getOsaamiset().stream()
            .map(ExtApiV1Mapper::toYksilonOsaaminenDto)
            .collect(Collectors.toSet());
    Set<ExtOsaamisKiinnostusDto> osaamisKiinnostukset =
        yksilo.getOsaamisKiinnostukset().stream()
            .map(ExtApiV1Mapper::toOsaamisKiinnostusDto)
            .collect(Collectors.toSet());
    Set<ExtAmmattiKiinnostusDto> ammattiKiinnostukset =
        yksilo.getAmmattiKiinnostukset().stream()
            .map(ExtApiV1Mapper::toAmmattiKiinnostusDto)
            .collect(Collectors.toSet());
    Set<ExtSuosikkiDto> suosikit =
        yksilo.getSuosikit().stream()
            .map(ExtApiV1Mapper::toSuosikkiDto)
            .collect(Collectors.toSet());
    Set<ExtPaamaaraDto> paamaarat =
        yksilo.getPaamaarat().stream()
            .map(ExtApiV1Mapper::toPaamaaraDto)
            .collect(Collectors.toSet());
    return new ExtProfiiliDto(
        id, yksilonOsaamiset, osaamisKiinnostukset, ammattiKiinnostukset, suosikit, paamaarat);
  }

  private static String hashedId(UUID id) {
    byte[] idBytes = uuidToBytes(id);
    try {
      final byte[] digest = MessageDigest.getInstance("SHA-256").digest(idBytes);
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      // This should not happen since we know SHA-256 algorithm exists
      throw new RuntimeException(e);
    }
  }

  private static ExtPaamaaraDto toPaamaaraDto(Paamaara paamaara) {
    var tyomahdollisuusId =
        paamaara.getTyomahdollisuus() != null ? paamaara.getTyomahdollisuus().getId() : null;
    var koulutusMahdollisuusId =
        paamaara.getKoulutusmahdollisuus() != null
            ? paamaara.getKoulutusmahdollisuus().getId()
            : null;
    return new ExtPaamaaraDto(paamaara.getTyyppi(), tyomahdollisuusId, koulutusMahdollisuusId);
  }

  private static ExtSuosikkiDto toSuosikkiDto(YksilonSuosikki yksilonSuosikki) {
    var tyomahdollisuusId =
        yksilonSuosikki.getTyomahdollisuus() != null
            ? yksilonSuosikki.getTyomahdollisuus().getId()
            : null;
    var koulutusMahdollisuusId =
        yksilonSuosikki.getKoulutusmahdollisuus() != null
            ? yksilonSuosikki.getKoulutusmahdollisuus().getId()
            : null;
    return new ExtSuosikkiDto(tyomahdollisuusId, koulutusMahdollisuusId);
  }

  private static ExtAmmattiKiinnostusDto toAmmattiKiinnostusDto(Ammatti ammatti) {
    return new ExtAmmattiKiinnostusDto(ammatti.getUri(), ammatti.getKoodi());
  }

  private static ExtOsaamisKiinnostusDto toOsaamisKiinnostusDto(Osaaminen osaaminen) {
    return new ExtOsaamisKiinnostusDto(osaaminen.getUri());
  }

  private static ExtYksilonOsaaminenDto toYksilonOsaaminenDto(YksilonOsaaminen yksilonOsaaminen) {
    return new ExtYksilonOsaaminenDto(
        yksilonOsaaminen.getLahdeTyyppi(), yksilonOsaaminen.getOsaaminen().getUri());
  }
}
