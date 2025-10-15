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
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaDto;
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaUpdateDto;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.repository.TyopaikkaRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceException;
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
public class TyopaikkaService {

  private final YksiloRepository yksilot;
  private final TyopaikkaRepository tyopaikat;
  private final ToimenkuvaService toimenkuvaService;

  @Transactional(readOnly = true)
  public List<TyopaikkaDto> findAll(JodUser user) {
    return tyopaikat.findByYksiloId(user.getId()).stream().map(Mapper::mapTyopaikka).toList();
  }

  @Transactional(readOnly = true)
  public TyopaikkaDto get(JodUser user, UUID id) {
    return tyopaikat
        .findByYksiloIdAndId(user.getId(), id)
        .map(Mapper::mapTyopaikka)
        .orElseThrow(TyopaikkaService::notFound);
  }

  public void delete(JodUser user, UUID id) {
    var tyopaikka =
        tyopaikat.findByYksiloIdAndId(user.getId(), id).orElseThrow(TyopaikkaService::notFound);
    for (var toimenkuva : tyopaikka.getToimenkuvat()) {
      toimenkuvaService.delete(toimenkuva);
    }
    tyopaikat.delete(tyopaikka);
  }

  public void update(JodUser user, TyopaikkaUpdateDto dto) {
    var entity =
        tyopaikat
            .findByYksiloIdAndId(user.getId(), dto.id())
            .orElseThrow(TyopaikkaService::notFound);
    entity.setNimi(dto.nimi());
    tyopaikat.save(entity);
  }

  public UUID add(JodUser user, TyopaikkaDto dto) {
    var yksilo = yksilot.getReferenceById(user.getId());
    if (tyopaikat.countByYksilo(yksilo) >= Limits.TYOPAIKKA) {
      throw new ServiceValidationException("Too many Tyopaikka");
    }
    // prevent duplicates somehow?
    var entity = tyopaikat.save(new Tyopaikka(yksilot.getReferenceById(user.getId()), dto.nimi()));
    if (dto.toimenkuvat() != null) {
      for (var toimenkuva : dto.toimenkuvat()) {
        entity.getToimenkuvat().add(toimenkuvaService.add(entity, toimenkuva));
      }
    }

    return entity.getId();
  }

  private static ServiceException notFound() {
    return new NotFoundException("Työpaikka not found");
  }
}
