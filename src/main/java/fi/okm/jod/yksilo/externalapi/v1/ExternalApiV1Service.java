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
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import fi.okm.jod.yksilo.externalapi.v1.dto.ExtApiV1Mapper;
import fi.okm.jod.yksilo.externalapi.v1.dto.ExtKoulutusMahdollisuusDto;
import fi.okm.jod.yksilo.externalapi.v1.dto.ExtProfiiliDto;
import fi.okm.jod.yksilo.externalapi.v1.dto.ExtTyoMahdollisuusDto;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bisneslogiikka External API V1 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExternalApiV1Service {
  private final TyomahdollisuusRepository tyomahdollisuusRepository;
  private final KoulutusmahdollisuusRepository koulutusmahdollisuusRepository;
  private final YksiloRepository yksiloRepository;

  public SivuDto<ExtTyoMahdollisuusDto> findTyomahdollisuudet(final Pageable pageable) {
    final Page<Tyomahdollisuus> tyomahdollisuusPage =
        this.tyomahdollisuusRepository.findAll(pageable);
    final List<ExtTyoMahdollisuusDto> tyoMahdollisuusDtoList =
        tyomahdollisuusPage.stream().map(ExtApiV1Mapper::toTyoMahdollisuusDto).toList();

    return new SivuDto<>(
        tyoMahdollisuusDtoList,
        tyomahdollisuusPage.getTotalElements(),
        tyomahdollisuusPage.getTotalPages());
  }

  public SivuDto<ExtKoulutusMahdollisuusDto> findKoulutusmahdollisuudet(final Pageable pageable) {
    final Page<Koulutusmahdollisuus> koulutusmahdollisuusPage =
        this.koulutusmahdollisuusRepository.findAll(pageable);
    final List<ExtKoulutusMahdollisuusDto> koulutusMahdollisuusDtoList =
        koulutusmahdollisuusPage.stream().map(ExtApiV1Mapper::toKoulutusMahdollisuusDto).toList();

    return new SivuDto<>(
        koulutusMahdollisuusDtoList,
        koulutusmahdollisuusPage.getTotalElements(),
        koulutusmahdollisuusPage.getTotalPages());
  }

  public SivuDto<ExtProfiiliDto> findYksilot(final Instant modifiedAfter, final Pageable pageable) {
    return new SivuDto<>(getYksiloPage(modifiedAfter, pageable).map(ExtApiV1Mapper::toProfiiliDto));
  }

  private Page<Yksilo> getYksiloPage(final Instant modifiedAfter, final Pageable pageable) {
    if (modifiedAfter == null) {
      return this.yksiloRepository.findAll(pageable);
    }
    return this.yksiloRepository.findAllModifiedAfter(modifiedAfter, pageable);
  }
}
