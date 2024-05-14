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
import fi.okm.jod.yksilo.dto.TyopaikkaDto;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.repository.TyopaikkaRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
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

  public List<TyopaikkaDto> findAll(JodUser user) {
    return tyopaikat.findByYksiloId(user.getId()).stream().map(Mapper::mapTyopaikka).toList();
  }

  public void delete(JodUser user, UUID id) {
    tyopaikat.deleteByYksiloIdAndId(user.getId(), id);
  }

  public void update(JodUser user, TyopaikkaDto dto) {
    var entity =
        tyopaikat
            .findByYksiloIdAndId(user.getId(), dto.id())
            .orElseThrow(() -> new NotFoundException("Työpaikka not found"));
    entity.setNimi(dto.nimi());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    tyopaikat.save(entity);
  }

  public UUID add(JodUser user, TyopaikkaDto dto) {
    // prevent duplicates somehow?
    var entity = new Tyopaikka(yksilot.getReferenceById(user.getId()), dto.nimi());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    return tyopaikat.save(entity).getId();
  }
}
