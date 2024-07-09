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
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.repository.TyopaikkaRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.validation.Limits;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

  public List<TyopaikkaDto> findAll(JodUser user) {
    return tyopaikat.findByYksiloId(user.getId()).stream().map(Mapper::mapTyopaikka).toList();
  }

  public TyopaikkaDto find(JodUser user, UUID id) {
    return tyopaikat
        .findByYksiloIdAndId(user.getId(), id)
        .map(Mapper::mapTyopaikka)
        .orElseThrow(() -> new NotFoundException("Työpaikka not found"));
  }

  public void delete(JodUser user, UUID id) {
    var tyopaikka =
        tyopaikat
            .findByYksiloIdAndId(user.getId(), id)
            .orElseThrow(() -> new NotFoundException("Työpaikka not found"));
    for (var toimenkuva : tyopaikka.getToimenkuvat()) {
      toimenkuvaService.delete(toimenkuva);
    }
    tyopaikat.delete(tyopaikka);
  }

  public void update(JodUser user, TyopaikkaDto dto) {
    var entity =
        tyopaikat
            .findByYksiloIdAndId(user.getId(), dto.id())
            .orElseThrow(() -> new NotFoundException("Työpaikka not found"));
    entity.setNimi(dto.nimi());
    tyopaikat.save(entity);
    var toimenkuvat = dto.toimenkuvat();
    if (toimenkuvat != null) {
      Set<UUID> ids = new HashSet<>();
      for (var toimenkuva : toimenkuvat) {
        if (toimenkuva.id() == null) {
          var t = toimenkuvaService.add(entity, toimenkuva);
          entity.getToimenkuvat().add(t);
          ids.add(t.getId());
        } else {
          toimenkuvaService.update(user, entity.getId(), toimenkuva);
          ids.add(toimenkuva.id());
        }
      }
      for (var it = entity.getToimenkuvat().iterator(); it.hasNext(); ) {
        var t = it.next();
        if (!ids.contains(t.getId())) {
          toimenkuvaService.delete(t);
          it.remove();
        }
      }
    }
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
}
