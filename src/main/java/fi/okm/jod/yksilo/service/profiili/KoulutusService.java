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
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.repository.KoulutusKokonaisuusRepository;
import fi.okm.jod.yksilo.repository.KoulutusRepository;
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
public class KoulutusService {
  private final KoulutusKokonaisuusRepository kokonaisuudet;
  private final KoulutusRepository koulutukset;
  private final YksilonOsaaminenService osaamiset;

  public List<KoulutusDto> findAll(JodUser user, UUID kokonaisuusId) {
    return koulutukset
        .findByKokonaisuusYksiloIdAndKokonaisuusId(user.getId(), kokonaisuusId)
        .stream()
        .map(Mapper::mapKoulutus)
        .toList();
  }

  public UUID add(JodUser user, UUID kokonaisuusId, KoulutusDto dto) {
    var kokonaisuus =
        kokonaisuudet
            .findByYksiloIdAndId(user.getId(), kokonaisuusId)
            .orElseThrow(KoulutusService::notFound);

    if (koulutukset.countByKokonaisuus(kokonaisuus) >= Limits.KOULUTUS_PER_KOKONAISUUS) {
      throw new ServiceValidationException("Too many Koulutus");
    }

    return add(kokonaisuus, dto).getId();
  }

  public KoulutusDto get(JodUser user, UUID tyopaikkaId, UUID id) {
    return koulutukset
        .findByKokonaisuusYksiloIdAndKokonaisuusIdAndId(user.getId(), tyopaikkaId, id)
        .map(Mapper::mapKoulutus)
        .orElseThrow(KoulutusService::notFound);
  }

  public void update(JodUser user, UUID kokonaisuus, KoulutusDto dto) {
    var entity =
        koulutukset
            .findByKokonaisuusYksiloIdAndKokonaisuusIdAndId(user.getId(), kokonaisuus, dto.id())
            .orElseThrow(KoulutusService::notFound);
    update(entity, dto);
  }

  public void delete(JodUser user, UUID kokonaisuusId, UUID id) {
    var koulutus =
        koulutukset
            .findByKokonaisuusYksiloIdAndKokonaisuusIdAndId(user.getId(), kokonaisuusId, id)
            .orElseThrow(KoulutusService::notFound);
    delete(koulutus);
    kokonaisuudet.deleteEmpty(user.getId(), kokonaisuusId);
  }

  Koulutus add(KoulutusKokonaisuus kokonaisuus, KoulutusDto dto) {
    var entity = new Koulutus(kokonaisuus);
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    entity = koulutukset.save(entity);
    if (dto.osaamiset() != null) {
      osaamiset.add(entity, dto.osaamiset());
    }
    return entity;
  }

  void update(Koulutus entity, KoulutusDto dto) {
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    koulutukset.save(entity);
    if (dto.osaamiset() != null) {
      osaamiset.update(entity, dto.osaamiset());
    }
  }

  void delete(Koulutus koulutus) {
    osaamiset.deleteAll(koulutus.getOsaamiset());
    koulutukset.delete(koulutus);
  }

  static NotFoundException notFound() {
    return new NotFoundException("Not found");
  }
}
