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
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.repository.ToimintoRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.validation.Limits;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ToimintoService {

  private final YksiloRepository yksilot;
  private final ToimintoRepository toiminnot;
  private final PatevyysService patevyysService;

  public List<ToimintoDto> findAll(JodUser user) {
    return toiminnot.findByYksiloId(user.getId()).stream().map(Mapper::mapToiminto).toList();
  }

  public ToimintoDto find(JodUser user, UUID id) {
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
    // prevent duplicates somehow?
    var entity = toiminnot.save(new Toiminto(yksilot.getReferenceById(user.getId()), dto.nimi()));
    if (dto.patevyydet() != null) {
      for (var patevyys : dto.patevyydet()) {
        patevyysService.add(entity, patevyys);
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
    toiminnot.save(entity);
    var patevyydet = dto.patevyydet();
    if (patevyydet != null) {
      patevyydet.forEach(patevyysDto -> patevyysService.update(user, entity.getId(), patevyysDto));
    }
  }

  public void delete(JodUser user, Set<UUID> ids) {
    // Note. Bypasses persistence context
    if (toiminnot.deleteByYksiloIdAndIdIn(user.getId(), ids) != ids.size()) {
      throw new NotFoundException("Not found");
    }
  }
}
