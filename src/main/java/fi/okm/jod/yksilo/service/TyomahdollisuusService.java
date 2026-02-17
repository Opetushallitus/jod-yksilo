/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import static fi.okm.jod.yksilo.service.JakaumaMapper.mapJakauma;

import fi.okm.jod.yksilo.dto.tyomahdollisuus.AmmattiryhmaBasicDto;
import fi.okm.jod.yksilo.dto.tyomahdollisuus.AmmattiryhmaFullDto;
import fi.okm.jod.yksilo.dto.tyomahdollisuus.KoulutusAlaDto;
import fi.okm.jod.yksilo.dto.tyomahdollisuus.KoulutusasteDto;
import fi.okm.jod.yksilo.dto.tyomahdollisuus.MaakuntaDto;
import fi.okm.jod.yksilo.dto.tyomahdollisuus.TyollisyysDto;
import fi.okm.jod.yksilo.dto.tyomahdollisuus.TyomahdollisuusDto;
import fi.okm.jod.yksilo.dto.tyomahdollisuus.TyomahdollisuusFullDto;
import fi.okm.jod.yksilo.entity.Ammattiryhma;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus_;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TyomahdollisuusService {
  private final TyomahdollisuusRepository tyomahdollisuusRepository;

  public Page<TyomahdollisuusDto> findAll(Pageable pageable) {
    return tyomahdollisuusRepository
        .findAll(
            PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Tyomahdollisuus_.ID)))
        .map(TyomahdollisuusService::map);
  }

  public List<TyomahdollisuusDto> findByIds(Set<UUID> uuidSet) {
    return tyomahdollisuusRepository.findAllById(uuidSet).stream()
        .map(TyomahdollisuusService::map)
        .toList();
  }

  public TyomahdollisuusFullDto get(UUID id) {
    Tyomahdollisuus tyomahdollisuus =
        tyomahdollisuusRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Unknown tyomahdollisuus"));
    Ammattiryhma ammattiryhma = tyomahdollisuus.getAmmattiryhma();
    return mapFull(tyomahdollisuus, ammattiryhma);
  }

  private static TyomahdollisuusDto map(Tyomahdollisuus entity) {
    if (entity == null) {
      return null;
    }
    final AmmattiryhmaBasicDto ammattiryhmaBasicDto = getAmmattiryhmaBasicDto(entity);
    return new TyomahdollisuusDto(
        entity.getId(),
        entity.getOtsikko(),
        entity.getTiivistelma(),
        entity.getKuvaus(),
        ammattiryhmaBasicDto,
        entity.getAineisto(),
        entity.isAktiivinen());
  }

  private static AmmattiryhmaBasicDto getAmmattiryhmaBasicDto(final Tyomahdollisuus entity) {
    AmmattiryhmaBasicDto ammattiryhmaBasicDto;
    final Ammattiryhma ammattiryhma = entity.getAmmattiryhma();
    Integer mediaaniPalkka = null;
    String kohtaanto = null;
    if (ammattiryhma != null) {
      mediaaniPalkka = ammattiryhma.getMediaaniPalkka();
      kohtaanto = ammattiryhma.getKohtaanto();
    }
    ammattiryhmaBasicDto =
        new AmmattiryhmaBasicDto(entity.getAmmattiryhmaUri(), mediaaniPalkka, kohtaanto);
    return ammattiryhmaBasicDto;
  }

  private static TyomahdollisuusFullDto mapFull(
      final Tyomahdollisuus entity, final Ammattiryhma ammattiryhma) {
    return entity == null
        ? null
        : new TyomahdollisuusFullDto(
            entity.getId(),
            entity.getOtsikko(),
            entity.getTiivistelma(),
            entity.getKuvaus(),
            entity.getTehtavat(),
            entity.getYleisetVaatimukset(),
            mapAmmattiryhma(entity.getAmmattiryhmaUri(), ammattiryhma),
            entity.getAineisto(),
            entity.isAktiivinen(),
            entity.getJakaumat().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> mapJakauma(e.getValue()))));
  }

  private static AmmattiryhmaFullDto mapAmmattiryhma(
      final URI ammattiryhmaUri, final Ammattiryhma ammattiryhma) {
    if (ammattiryhma == null && ammattiryhmaUri == null) {
      return null;
    } else if (ammattiryhma == null) {
      return new AmmattiryhmaFullDto(ammattiryhmaUri, null, null, null, null, null);
    }
    final TyollisyysDto tyollisyysData =
        new TyollisyysDto(
            ammattiryhma.getTyollistenMaara(),
            getTyollisetKoulutusAloittain(ammattiryhma),
            getTyollisetKoulutusAsteittain(ammattiryhma),
            getTyollisetMaakunnittain(ammattiryhma));
    return new AmmattiryhmaFullDto(
        ammattiryhmaUri,
        ammattiryhma.getMediaaniPalkka(),
        ammattiryhma.getYlinDesiiliPalkka(),
        ammattiryhma.getAlinDesiiliPalkka(),
        tyollisyysData,
        ammattiryhma.getKohtaanto());
  }

  private static List<KoulutusAlaDto> getTyollisetKoulutusAloittain(
      final Ammattiryhma ammattiryhma) {
    var node = ammattiryhma.getData().path("tyollisetKoulutusAloittain");
    if (node.isMissingNode() || !node.isArray()) {
      return new ArrayList<>();
    }

    List<KoulutusAlaDto> result = new ArrayList<>();
    for (var entry : node) {
      if (entry.path("osuus").isNumber()) {
        result.add(
            new KoulutusAlaDto(entry.path("koulutusala").asText(), entry.path("osuus").asDouble()));
      }
    }
    return result;
  }

  private static List<KoulutusasteDto> getTyollisetKoulutusAsteittain(
      final Ammattiryhma ammattiryhma) {
    var node = ammattiryhma.getData().path("tyollisetKoulutusAsteittain");
    if (node.isMissingNode() || !node.isArray()) {
      return new ArrayList<>();
    }

    List<KoulutusasteDto> result = new ArrayList<>();
    for (var entry : node) {
      if (entry.path("osuus").isNumber()) {
        result.add(
            new KoulutusasteDto(
                entry.path("koulutusaste").asText(), entry.path("osuus").asDouble()));
      }
    }
    return result;
  }

  private static List<MaakuntaDto> getTyollisetMaakunnittain(final Ammattiryhma ammattiryhma) {
    var node = ammattiryhma.getData().path("tyollisetMaakunnittain");
    if (node.isMissingNode() || !node.isArray()) {
      return new ArrayList<>();
    }

    List<MaakuntaDto> result = new ArrayList<>();
    for (var entry : node) {
      if (entry.path("osuus").isNumber()) {
        result.add(
            new MaakuntaDto(entry.path("maakunta").asText(), entry.path("osuus").asDouble()));
      }
    }
    return result;
  }
}
