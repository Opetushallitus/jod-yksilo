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
import fi.okm.jod.yksilo.dto.profiili.ToimenkuvaDto;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.repository.ToimenkuvaRepository;
import fi.okm.jod.yksilo.repository.TyopaikkaRepository;
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
public class ToimenkuvaService {
  private final TyopaikkaRepository tyopaikat;
  private final ToimenkuvaRepository toimenkuvat;
  private final YksilonOsaaminenService osaamiset;

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

    if (toimenkuvat.countByTyopaikka(tyopaikka) >= Limits.TOIMENKUVA_PER_TYOPAIKKA) {
      throw new ServiceValidationException("Too many Toimenkuva");
    }

    return add(tyopaikka, dto).getId();
  }

  Toimenkuva add(Tyopaikka tyopaikka, ToimenkuvaDto dto) {
    var entity = new Toimenkuva(tyopaikka);
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    var toimenkuva = toimenkuvat.save(entity);
    if (dto.osaamiset() != null) {
      osaamiset.add(toimenkuva, dto.osaamiset());
    }
    return toimenkuva;
  }

  public void update(JodUser user, UUID tyopaikka, ToimenkuvaDto dto) {
    var entity =
        toimenkuvat.findBy(user, tyopaikka, dto.id()).orElseThrow(ToimenkuvaService::notFound);
    update(entity, dto);
  }

  void update(Toimenkuva entity, ToimenkuvaDto dto) {
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    toimenkuvat.save(entity);
    if (dto.osaamiset() != null) {
      osaamiset.update(entity, dto.osaamiset());
    }
  }

  public void delete(JodUser user, UUID tyopaikka, UUID id) {
    var toimenkuva =
        toimenkuvat.findBy(user, tyopaikka, id).orElseThrow(ToimenkuvaService::notFound);
    delete(toimenkuva);
  }

  void delete(Toimenkuva toimenkuva) {
    osaamiset.deleteAll(toimenkuva.getOsaamiset());
    toimenkuvat.delete(toimenkuva);
  }

  static NotFoundException notFound() {
    return new NotFoundException("Not found");
  }
}
