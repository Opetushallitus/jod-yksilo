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
import fi.okm.jod.yksilo.dto.ToimenkuvaDto;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.repository.ToimenkuvaRepository;
import fi.okm.jod.yksilo.repository.TyopaikkaRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ToimenkuvaService {
  private final TyopaikkaRepository tyopaikat;
  private final ToimenkuvaRepository toimenkuvat;

  public List<ToimenkuvaDto> findAll(JodUser user, UUID tyopaikkaId) {
    return tyopaikat
        .findByYksiloIdAndId(user.getId(), tyopaikkaId)
        .orElseThrow(ToimenkuvaService::notFound)
        .getToimenkuvat()
        .stream()
        .map(Mapper::mapToimenkuva)
        .toList();
  }

  public ToimenkuvaDto get(JodUser user, UUID tyopaikkaId, UUID id) {
    return toimenkuvat
        .findBy(user, tyopaikkaId, id)
        .map(Mapper::mapToimenkuva)
        .orElseThrow(ToimenkuvaService::notFound);
  }

  public UUID add(JodUser user, UUID tyopaikkaId, ToimenkuvaDto dto) {
    var tyopaikka =
        tyopaikat
            .findByYksiloIdAndId(user.getId(), tyopaikkaId)
            .orElseThrow(ToimenkuvaService::notFound);
    var entity = new Toimenkuva(tyopaikka);
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    return toimenkuvat.save(entity).getId();
  }

  public void update(JodUser user, UUID tyopaikka, ToimenkuvaDto dto) {
    var entity =
        toimenkuvat.findBy(user, tyopaikka, dto.id()).orElseThrow(ToimenkuvaService::notFound);
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    toimenkuvat.save(entity);
  }

  public void delete(JodUser user, UUID tyopaikka, UUID id) {
    if (toimenkuvat.delete(user, tyopaikka, id) != 1) {
      throw notFound();
    }
  }

  static NotFoundException notFound() {
    return new NotFoundException("Not found");
  }
}
