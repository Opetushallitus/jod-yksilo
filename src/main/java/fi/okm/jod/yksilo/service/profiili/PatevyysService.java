/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.PatevyysDto;
import fi.okm.jod.yksilo.entity.Patevyys;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.repository.PatevyysRepository;
import fi.okm.jod.yksilo.repository.ToimintoRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.validation.Limits;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PatevyysService {
  private final ToimintoRepository toiminnot;
  private final PatevyysRepository patevyydet;
  private final YksilonOsaaminenService osaamiset;

  public UUID add(JodUser user, UUID toimintoId, PatevyysDto dto) {
    var toiminto =
        toiminnot
            .findByYksiloIdAndId(user.getId(), toimintoId)
            .orElseThrow(ToimenkuvaService::notFound);

    if (patevyydet.countByToiminto(toiminto) >= Limits.PATEVYYS_PER_TOIMINTO) {
      throw new ServiceValidationException("Too many Pätevyys");
    }

    return add(toiminto, dto).getId();
  }

  Patevyys add(Toiminto toiminto, PatevyysDto dto) {
    var entity = new Patevyys(toiminto);
    entity.setNimi(dto.nimi());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    var toimenkuva = patevyydet.save(entity);
    if (dto.osaamiset() != null) {
      osaamiset.add(toimenkuva, dto.osaamiset());
    }
    return toimenkuva;
  }

  public void update(JodUser user, UUID toiminto, PatevyysDto dto) {
    var entity = patevyydet.findBy(user, toiminto, dto.id()).orElseThrow(PatevyysService::notFound);
    entity.setNimi(dto.nimi());
    patevyydet.save(entity);
    if (dto.osaamiset() != null) {
      osaamiset.update(entity, dto.osaamiset());
    }
  }

  static NotFoundException notFound() {
    return new NotFoundException("Not found");
  }
}
