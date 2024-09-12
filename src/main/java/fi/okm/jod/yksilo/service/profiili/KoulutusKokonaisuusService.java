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
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.repository.KoulutusKokonaisuusRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.validation.Limits;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class KoulutusKokonaisuusService {

  private final YksiloRepository yksilot;
  private final KoulutusKokonaisuusRepository kokonaisuudet;
  private final KoulutusService koulutusService;
  private final Updater<KoulutusKokonaisuus, Koulutus, KoulutusDto> updater;

  public KoulutusKokonaisuusService(
      YksiloRepository yksilot,
      KoulutusKokonaisuusRepository kokonaisuudet,
      KoulutusService koulutusService) {
    this.yksilot = yksilot;
    this.kokonaisuudet = kokonaisuudet;
    this.koulutusService = koulutusService;
    this.updater =
        new Updater<>(koulutusService::add, koulutusService::update, koulutusService::delete);
  }

  public List<KoulutusKokonaisuusDto> findAll(JodUser user) {
    return kokonaisuudet.findByYksiloId(user.getId()).stream()
        .map(Mapper::mapKoulutusKokonaisuus)
        .toList();
  }

  public UUID add(JodUser user, KoulutusKokonaisuusDto dto) {
    var yksilo = yksilot.getReferenceById(user.getId());
    if (kokonaisuudet.countByYksilo(yksilo) >= Limits.KOULUTUSKOKONAISUUS) {
      throw new ServiceValidationException("Too many KoulutusKokonaisuus");
    }
    // prevent duplicates somehow?
    var entity =
        kokonaisuudet.save(
            new KoulutusKokonaisuus(yksilot.getReferenceById(user.getId()), dto.nimi(), null));
    if (dto.koulutukset() != null) {
      for (var koulutus : dto.koulutukset()) {
        entity.getKoulutukset().add(koulutusService.add(entity, koulutus));
      }
    }

    return entity.getId();
  }

  public KoulutusKokonaisuusDto get(JodUser user, UUID id) {
    return kokonaisuudet
        .findByYksiloIdAndId(user.getId(), id)
        .map(Mapper::mapKoulutusKokonaisuus)
        .orElseThrow(KoulutusKokonaisuusService::notFound);
  }

  public void update(JodUser user, KoulutusKokonaisuusDto dto) {
    var entity =
        kokonaisuudet
            .findByYksiloIdAndId(user.getId(), dto.id())
            .orElseThrow(KoulutusKokonaisuusService::notFound);
    entity.setNimi(dto.nimi());
    kokonaisuudet.save(entity);

    if (dto.koulutukset() != null
        && !updater.merge(entity, entity.getKoulutukset(), dto.koulutukset())) {
      throw new ServiceValidationException("Invalid Koulutus in Update");
    }
  }

  public void delete(JodUser user, UUID id) {
    var kokonaisuus =
        kokonaisuudet
            .findByYksiloIdAndId(user.getId(), id)
            .orElseThrow(KoulutusKokonaisuusService::notFound);
    for (var koulutus : kokonaisuus.getKoulutukset()) {
      koulutusService.delete(koulutus);
    }
    kokonaisuudet.delete(kokonaisuus);
  }

  private static ServiceException notFound() {
    return new NotFoundException("KoulutusKokonaisuus not found");
  }
}
