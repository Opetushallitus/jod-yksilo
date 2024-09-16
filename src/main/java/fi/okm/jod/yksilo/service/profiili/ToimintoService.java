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
import fi.okm.jod.yksilo.dto.profiili.ToimintoUpdateDto;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.repository.ToimintoRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
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
public class ToimintoService {

  private final YksiloRepository yksilot;
  private final ToimintoRepository toiminnot;
  private final PatevyysService patevyysService;

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
    var toiminto = toiminnot.save(new Toiminto(yksilot.getReferenceById(user.getId()), dto.nimi()));
    if (dto.patevyydet() != null) {
      for (var patevyys : dto.patevyydet()) {
        toiminto.getPatevyydet().add(patevyysService.add(toiminto, patevyys));
      }
    }
    return toiminto.getId();
  }

  public void update(JodUser user, ToimintoUpdateDto dto) {
    var toiminto =
        toiminnot
            .findByYksiloIdAndId(user.getId(), dto.id())
            .orElseThrow(() -> new NotFoundException("Toiminto not found"));
    toiminto.setNimi(dto.nimi());
  }

  public void delete(JodUser user, UUID id) {
    var toiminto =
        toiminnot
            .findByYksiloIdAndId(user.getId(), id)
            .orElseThrow(() -> new NotFoundException("Toiminto not found"));
    for (var patevyys : toiminto.getPatevyydet()) {
      patevyysService.delete(patevyys);
    }
    toiminnot.delete(toiminto);
  }
}
