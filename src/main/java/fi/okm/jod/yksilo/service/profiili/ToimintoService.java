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
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.entity.Teema;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.TeemaRepository;
import fi.okm.jod.yksilo.repository.ToimintoRepository;
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
public class ToimintoService {
  private final TeemaRepository teemat;
  private final ToimintoRepository toiminnot;
  private final YksilonOsaaminenService osaamiset;

  @Transactional(readOnly = true)
  public List<ToimintoDto> findAll(JodUser user, UUID teemaId) {
    return toiminnot.findByTeemaYksiloIdAndTeemaId(user.getId(), teemaId).stream()
        .map(Mapper::mapToiminto)
        .toList();
  }

  public UUID add(JodUser user, UUID teemaId, ToimintoDto dto) {
    var teema =
        teemat.findByYksiloIdAndId(user.getId(), teemaId).orElseThrow(ToimenkuvaService::notFound);

    if (toiminnot.countByTeemaYksilo(teema.getYksilo()) >= Limits.TOIMINTO) {
      throw new ProfileLimitException(ProfileItem.TOIMINTO);
    }

    teema.setTuontiLahde(null);
    return add(teema, dto).getId();
  }

  @Transactional(readOnly = true)
  public ToimintoDto get(JodUser user, UUID teemaId, UUID id) {
    return toiminnot
        .findBy(user, teemaId, id)
        .map(Mapper::mapToiminto)
        .orElseThrow(ToimintoService::notFound);
  }

  @Transactional(readOnly = true)
  public Map<UUID, ToimintoDto> findAllByIds(JodUser user, Set<UUID> ids) {
    return toiminnot.findByTeemaYksiloIdAndIdIn(user.getId(), ids).stream()
        .collect(Collectors.toMap(Toiminto::getId, Mapper::mapToiminto));
  }

  public void update(JodUser user, UUID teemaId, ToimintoDto dto) {
    var entity = toiminnot.findBy(user, teemaId, dto.id()).orElseThrow(ToimintoService::notFound);
    update(entity, dto);
  }

  public void delete(JodUser user, UUID teemaId, UUID toimintoId) {
    var entity = toiminnot.findBy(user, teemaId, toimintoId).orElseThrow(ToimintoService::notFound);
    delete(entity);
    teemat.deleteEmpty(user.getId(), teemaId);
  }

  long countBy(Yksilo yksilo) {
    return toiminnot.countByTeemaYksilo(yksilo);
  }

  Toiminto add(Teema teema, ToimintoDto dto) {
    var entity = new Toiminto(teema);
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    var toiminto = toiminnot.save(entity);
    if (dto.osaamiset() != null) {
      osaamiset.addLahteenOsaamiset(toiminto, osaamiset.getOsaamiset(dto.osaamiset()));
    }
    return toiminto;
  }

  void update(Toiminto entity, ToimintoDto dto) {
    entity.getTeema().setTuontiLahde(null);
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    toiminnot.save(entity);
    if (dto.osaamiset() != null) {
      osaamiset.updateLahteenOsaamiset(entity, osaamiset.getOsaamiset(dto.osaamiset()));
    }
  }

  void delete(Toiminto toiminto) {
    toiminto.getTeema().setTuontiLahde(null);
    osaamiset.deleteAll(toiminto.getOsaamiset());
    toiminnot.delete(toiminto);
  }

  static NotFoundException notFound() {
    return new NotFoundException("Not found");
  }
}
