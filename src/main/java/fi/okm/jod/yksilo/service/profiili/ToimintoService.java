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
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.entity.Patevyys;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.repository.ToimintoRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.validation.Limits;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ToimintoService {

  private final YksiloRepository yksilot;
  private final ToimintoRepository toiminnot;
  private final PatevyysService patevyysService;
  private final Updater<Toiminto, Patevyys, PatevyysDto> updater;

  public ToimintoService(
      YksiloRepository yksilot, ToimintoRepository toiminnot, PatevyysService patevyysService) {
    this.yksilot = yksilot;
    this.toiminnot = toiminnot;
    this.patevyysService = patevyysService;
    this.updater =
        new Updater<>(patevyysService::add, patevyysService::update, patevyysService::delete);
  }

  public List<ToimintoDto> findAll(JodUser user) {
    return toiminnot.findByYksiloId(user.getId()).stream().map(Mapper::mapToiminto).toList();
  }

  public ToimintoDto get(JodUser user, UUID id) {
    return toiminnot
        .findByYksiloIdAndId(user.getId(), id)
        .map(Mapper::mapToiminto)
        .orElseThrow(KoulutusService::notFound);
  }

  public UUID add(JodUser user, ToimintoDto dto) {
    var yksilo = yksilot.getReferenceById(user.getId());
    if (toiminnot.countByYksilo(yksilo) >= Limits.TOIMINTO) {
      throw new ServiceValidationException("Too many Toiminto");
    }
    var entity = toiminnot.save(new Toiminto(yksilot.getReferenceById(user.getId()), dto.nimi()));
    if (dto.patevyydet() != null) {
      for (var patevyys : dto.patevyydet()) {
        entity.getPatevyydet().add(patevyysService.add(entity, patevyys));
      }
    }
    return entity.getId();
  }

  public void update(JodUser user, ToimintoDto dto) {

    var entity =
        toiminnot
            .findByYksiloIdAndId(user.getId(), dto.id())
            .orElseThrow(() -> new NotFoundException("Toiminto not found"));
    entity.setNimi(dto.nimi());

    if (dto.patevyydet() != null
        && !updater.merge(entity, entity.getPatevyydet(), dto.patevyydet())) {
      throw new ServiceValidationException("Invalid Patevyys in Update");
    }
  }

  public void delete(JodUser user, Set<UUID> ids) {
    this.toiminnot.findByYksiloIdAndIdIn(user.getId(), ids).stream()
        .map(Toiminto::getPatevyydet)
        .flatMap(List::stream)
        .forEach(patevyysService::delete);
    if (this.toiminnot.deleteByYksiloIdAndIdIn(user.getId(), ids) != ids.size()) {
      throw new NotFoundException("Not found");
    }
  }
}
