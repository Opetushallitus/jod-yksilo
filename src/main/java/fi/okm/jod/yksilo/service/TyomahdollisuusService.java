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
import fi.okm.jod.yksilo.dto.tyomahdollisuus.PalkkaDataDto;
import fi.okm.jod.yksilo.dto.tyomahdollisuus.TyomahdollisuusDto;
import fi.okm.jod.yksilo.dto.tyomahdollisuus.TyomahdollisuusFullDto;
import fi.okm.jod.yksilo.entity.Ammattiryhma;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus_;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import java.net.URI;
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
    AmmattiryhmaBasicDto ammattiryhmaBasicDto = null;
    final Ammattiryhma ammattiryhma = entity.getAmmattiryhma();
    if (ammattiryhma != null) {
      ammattiryhmaBasicDto =
          new AmmattiryhmaBasicDto(
              entity.getAmmattiryhma().getEscoUri(), ammattiryhma.getMediaaniPalkka());
    }
    return entity == null
        ? null
        : new TyomahdollisuusDto(
            entity.getId(),
            entity.getOtsikko(),
            entity.getTiivistelma(),
            entity.getKuvaus(),
            ammattiryhmaBasicDto,
            entity.getAineisto(),
            entity.isAktiivinen());
  }

  private static TyomahdollisuusFullDto mapFull(
      final Tyomahdollisuus entity, final Ammattiryhma ammattiryhma) {
    URI ammattiryhmaUri = ammattiryhma != null ? ammattiryhma.getUri() : null;
    return entity == null
        ? null
        : new TyomahdollisuusFullDto(
            entity.getId(),
            entity.getOtsikko(),
            entity.getTiivistelma(),
            entity.getKuvaus(),
            entity.getTehtavat(),
            entity.getYleisetVaatimukset(),
            ammattiryhmaUri,
            mapPalkkaData(ammattiryhma),
            entity.getAineisto(),
            entity.isAktiivinen(),
            entity.getJakaumat().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> mapJakauma(e.getValue()))));
  }

  private static PalkkaDataDto mapPalkkaData(final Ammattiryhma ammattiryhma) {
    if (ammattiryhma == null) {
      return null;
    }
    return new PalkkaDataDto(
        ammattiryhma.getMuokattu(),
        ammattiryhma.getMediaaniPalkka(),
        ammattiryhma.getYlinDesiiliPalkka(),
        ammattiryhma.getAlinDesiiliPalkka());
  }
}
