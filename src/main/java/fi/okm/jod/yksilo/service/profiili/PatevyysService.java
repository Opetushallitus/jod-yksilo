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
import java.util.List;
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

  public List<PatevyysDto> findAll(JodUser user, UUID toimintoId) {
    return patevyydet.findByToimintoYksiloIdAndToimintoId(user.getId(), toimintoId).stream()
        .map(Mapper::mapPatevyys)
        .toList();
  }

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

  public PatevyysDto get(JodUser user, UUID id, UUID patevyysId) {
    return patevyydet
        .findBy(user, id, patevyysId)
        .map(Mapper::mapPatevyys)
        .orElseThrow(PatevyysService::notFound);
  }

  public void update(JodUser user, UUID toimintoId, PatevyysDto dto) {
    var entity =
        patevyydet.findBy(user, toimintoId, dto.id()).orElseThrow(PatevyysService::notFound);
    update(entity, dto);
  }

  public void delete(JodUser user, UUID toimintoId, UUID patevyysId) {
    var entity =
        patevyydet.findBy(user, toimintoId, patevyysId).orElseThrow(PatevyysService::notFound);
    delete(entity);
    toiminnot.deleteEmpty(user.getId(), toimintoId);
  }

  Patevyys add(Toiminto toiminto, PatevyysDto dto) {
    var entity = new Patevyys(toiminto);
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    var patevyys = patevyydet.save(entity);
    osaamiset.addLahteenOsaamiset(patevyys, osaamiset.getOsaamiset(dto.osaamiset()));
    return patevyys;
  }

  void update(Patevyys entity, PatevyysDto dto) {
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    patevyydet.save(entity);
    if (dto.osaamiset() != null) {
      osaamiset.updateLahteenOsaamiset(entity, osaamiset.getOsaamiset(dto.osaamiset()));
    }
  }

  void delete(Patevyys patevyys) {
    osaamiset.deleteAll(patevyys.getOsaamiset());
    patevyydet.delete(patevyys);
  }

  static NotFoundException notFound() {
    return new NotFoundException("Not found");
  }
}
