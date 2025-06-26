/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.externalapi.v1;

import fi.okm.jod.yksilo.dto.SivuDto;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import fi.okm.jod.yksilo.externalapi.v1.dto.ExtAPIV1Mapper;
import fi.okm.jod.yksilo.externalapi.v1.dto.ExtKoulutusMahdollisuusDto;
import fi.okm.jod.yksilo.externalapi.v1.dto.ExtTyoMahdollisuusDto;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bisneslogiikka External API V1 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExternalApiV1Service {
  private final TyomahdollisuusRepository tyomahdollisuusRepository;
  private final KoulutusmahdollisuusRepository koulutusmahdollisuusRepository;

  public SivuDto<ExtTyoMahdollisuusDto> findTyoMahdollisuudet(final Pageable pageable) {
    final Page<Tyomahdollisuus> tyomahdollisuusPage =
        this.tyomahdollisuusRepository.findAll(pageable);
    final List<ExtTyoMahdollisuusDto> tyoMahdollisuusDtoList =
        tyomahdollisuusPage.stream().map(ExtAPIV1Mapper::toTyoMahdollisuusDto).toList();
    return new SivuDto<>(
        tyoMahdollisuusDtoList,
        tyomahdollisuusPage.getTotalElements(),
        tyomahdollisuusPage.getTotalPages());
  }

  public SivuDto<ExtKoulutusMahdollisuusDto> findKoulutusMahdollisuudet(final Pageable pageable) {
    final Page<Koulutusmahdollisuus> tyomahdollisuusPage =
        this.koulutusmahdollisuusRepository.findAll(pageable);
    final List<ExtKoulutusMahdollisuusDto> tyoMahdollisuusDtoList =
        tyomahdollisuusPage.stream().map(ExtAPIV1Mapper::toKoulutusMahdollisuusDto).toList();
    return new SivuDto<>(
        tyoMahdollisuusDtoList,
        tyomahdollisuusPage.getTotalElements(),
        tyomahdollisuusPage.getTotalPages());
  }
}
