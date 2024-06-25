/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import fi.okm.jod.yksilo.dto.TyomahdollisuusDto;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.service.profiili.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TyomahdollisuusService {
  private final TyomahdollisuusRepository tyomahdollisuusRepository;

  public Page<TyomahdollisuusDto> findAll(Pageable pageable) {
    return tyomahdollisuusRepository.findAll(pageable).map(Mapper::mapTyomahdollisuus);
  }
}
