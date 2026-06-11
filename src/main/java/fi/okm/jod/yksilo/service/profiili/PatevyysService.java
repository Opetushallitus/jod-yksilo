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
import fi.okm.jod.yksilo.entity.Teema;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.PatevyysRepository;
import fi.okm.jod.yksilo.repository.TeemaRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.profiili.ProfileLimitException.ProfileItem;
import fi.okm.jod.yksilo.validation.Limits;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PatevyysService {
  private final TeemaRepository teemat;
  private final PatevyysRepository patevyydet;
  private final YksilonOsaaminenService osaamiset;

  @Transactional(readOnly = true)
  public List<PatevyysDto> findAll(JodUser user, UUID teemaId) {
    return patevyydet.findByTeemaYksiloIdAndTeemaId(user.getId(), teemaId).stream()
        .map(Mapper::mapPatevyys)
        .toList();
  }

  public UUID add(JodUser user, UUID teemaId, PatevyysDto dto) {
    var teema =
        teemat.findByYksiloIdAndId(user.getId(), teemaId).orElseThrow(ToimenkuvaService::notFound);

    if (patevyydet.countByTeemaYksilo(teema.getYksilo()) >= Limits.PATEVYYS) {
      throw new ProfileLimitException(ProfileItem.PATEVYYS);
    }

    teema.setTuontiLahde(null);
    return add(teema, dto).getId();
  }

  @Transactional(readOnly = true)
  public PatevyysDto get(JodUser user, UUID teemaId, UUID id) {
    return patevyydet
        .findBy(user, teemaId, id)
        .map(Mapper::mapPatevyys)
        .orElseThrow(PatevyysService::notFound);
  }

  @Transactional(readOnly = true)
  public Map<UUID, PatevyysDto> findAllByIds(JodUser user, Set<UUID> ids) {
    return patevyydet.findByTeemaYksiloIdAndIdIn(user.getId(), ids).stream()
        .collect(Collectors.toMap(Patevyys::getId, Mapper::mapPatevyys));
  }

  public void update(JodUser user, UUID teemaId, PatevyysDto dto) {
    var entity = patevyydet.findBy(user, teemaId, dto.id()).orElseThrow(PatevyysService::notFound);
    update(entity, dto);
  }

  public void delete(JodUser user, UUID teemaId, UUID patevyysId) {
    var entity =
        patevyydet.findBy(user, teemaId, patevyysId).orElseThrow(PatevyysService::notFound);
    delete(entity);
    teemat.deleteEmpty(user.getId(), teemaId);
  }

  long countBy(Yksilo yksilo) {
    return patevyydet.countByTeemaYksilo(yksilo);
  }

  Patevyys add(Teema teema, PatevyysDto dto) {
    var entity = new Patevyys(teema);
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    var patevyys = patevyydet.save(entity);
    if (dto.osaamiset() != null) {
      osaamiset.addLahteenOsaamiset(patevyys, osaamiset.getOsaamiset(dto.osaamiset()));
    }
    return patevyys;
  }

  void update(Patevyys entity, PatevyysDto dto) {
    entity.getTeema().setTuontiLahde(null);
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
    patevyys.getTeema().setTuontiLahde(null);
    osaamiset.deleteAll(patevyys.getOsaamiset());
    patevyydet.delete(patevyys);
  }

  static NotFoundException notFound() {
    return new NotFoundException("Not found");
  }
}
