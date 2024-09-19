/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import fi.okm.jod.yksilo.dto.ArvoDto;
import fi.okm.jod.yksilo.dto.JakaumaDto;
import fi.okm.jod.yksilo.dto.TyomahdollisuusDto;
import fi.okm.jod.yksilo.dto.TyomahdollisuusFullDto;
import fi.okm.jod.yksilo.entity.Jakauma;
import fi.okm.jod.yksilo.entity.Jakauma.Arvo;
import fi.okm.jod.yksilo.entity.Tyomahdollisuus;
import fi.okm.jod.yksilo.entity.Tyomahdollisuus_;
import fi.okm.jod.yksilo.entity.projection.TyomahdollisuusMetadata;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TyomahdollisuusService {
  private final TyomahdollisuusRepository tyomahdollisuusRepository;

  static TyomahdollisuusDto map(Tyomahdollisuus entity) {
    return entity == null
        ? null
        : new TyomahdollisuusDto(
            entity.getId(), entity.getOtsikko(), entity.getTiivistelma(), entity.getKuvaus());
  }

  static TyomahdollisuusFullDto mapFull(Tyomahdollisuus entity) {
    return entity == null
        ? null
        : new TyomahdollisuusFullDto(
            entity.getId(),
            entity.getOtsikko(),
            entity.getTiivistelma(),
            entity.getKuvaus(),
            entity.getJakaumat().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> mapJakauma(e.getValue()))));
  }

  private static JakaumaDto mapJakauma(Jakauma jakauma) {
    return jakauma == null
        ? null
        : new JakaumaDto(jakauma.getMaara(), jakauma.getTyhjia(), mapArvot(jakauma.getArvot()));
  }

  private static List<ArvoDto> mapArvot(List<Arvo> arvot) {
    return arvot.stream().map(a -> new ArvoDto(a.arvo(), a.osuus())).toList();
  }

  @Cacheable("tyomahdollisuusMetadata")
  public Map<UUID, TyomahdollisuusMetadata> fetchAllTyomahdollisuusMetadata() {
    return tyomahdollisuusRepository.fetchAllTyomahdollisuusMetadata().stream()
        .collect(
            Collectors.toMap(
                TyomahdollisuusMetadata::id,
                Function.identity(),
                (existing, replacement) -> existing // Handle duplicates
                ));
  }

  public Page<TyomahdollisuusDto> findAll(Pageable pageable) {
    return tyomahdollisuusRepository
        .findAll(
            PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Tyomahdollisuus_.ID)))
        .map(TyomahdollisuusService::map);
  }

  public TyomahdollisuusFullDto findById(UUID id) {
    return mapFull(
        tyomahdollisuusRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Unknown tyomahdollisuus")));
  }

  public List<TyomahdollisuusDto> findByIds(Set<UUID> uuidSet) {
    return tyomahdollisuusRepository.findAllById(uuidSet).stream()
        .map(TyomahdollisuusService::map)
        .toList();
  }
}
