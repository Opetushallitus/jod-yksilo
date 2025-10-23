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

import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.dto.KestoJakaumaDto;
import fi.okm.jod.yksilo.dto.KoulutusViiteDto;
import fi.okm.jod.yksilo.dto.KoulutusmahdollisuusDto;
import fi.okm.jod.yksilo.dto.KoulutusmahdollisuusFullDto;
import fi.okm.jod.yksilo.entity.Jakauma;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus.KestoJakauma;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.KoulutusmahdollisuusJakauma;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus_;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
public class KoulutusmahdollisuusService {
  private final KoulutusmahdollisuusRepository koulutusmahdollisuudet;

  private static String mostCommonKoulutusala(Koulutusmahdollisuus entity) {
    final KoulutusmahdollisuusJakauma koulutusAlat =
        entity.getJakaumat().get(KoulutusmahdollisuusJakaumaTyyppi.KOULUTUSALA);
    if (koulutusAlat == null) {
      return null;
    }
    return koulutusAlat.getArvot().stream()
        .max(Comparator.comparingDouble(Jakauma.Arvo::osuus))
        .map(Jakauma.Arvo::arvo)
        .orElse(null);
  }

  private static KoulutusmahdollisuusDto map(Koulutusmahdollisuus entity) {

    return entity == null
        ? null
        : new KoulutusmahdollisuusDto(
            entity.getId(),
            entity.getTyyppi(),
            entity.getOtsikko(),
            entity.getTiivistelma(),
            entity.getKuvaus(),
            mapKesto(entity.getKesto()),
            mostCommonKoulutusala(entity),
            entity.isAktiivinen());
  }

  private static KoulutusmahdollisuusFullDto mapFull(Koulutusmahdollisuus entity) {
    return entity == null
        ? null
        : new KoulutusmahdollisuusFullDto(
            entity.getId(),
            entity.getTyyppi(),
            entity.getOtsikko(),
            entity.getTiivistelma(),
            entity.getKuvaus(),
            mapKesto(entity.getKesto()),
            entity.getKoulutukset().stream()
                .map(k -> new KoulutusViiteDto(k.getOid(), k.getNimi()))
                .collect(Collectors.toSet()),
            entity.getJakaumat().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> mapJakauma(e.getValue()))));
  }

  private static KestoJakaumaDto mapKesto(KestoJakauma kesto) {
    return kesto == null
        ? null
        : new KestoJakaumaDto(kesto.minimi(), kesto.mediaani(), kesto.maksimi());
  }

  public Page<KoulutusmahdollisuusDto> findAll(Pageable pageable) {
    return koulutusmahdollisuudet
        .findAll(
            PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Koulutusmahdollisuus_.ID)))
        .map(KoulutusmahdollisuusService::map);
  }

  public List<KoulutusmahdollisuusDto> findByIds(Set<UUID> uuidSet) {
    return koulutusmahdollisuudet.findAllById(uuidSet).stream()
        .map(KoulutusmahdollisuusService::map)
        .toList();
  }

  public KoulutusmahdollisuusFullDto get(UUID id) {
    return mapFull(
        koulutusmahdollisuudet
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Unknown Koulutusmahdollisuus")));
  }
}
