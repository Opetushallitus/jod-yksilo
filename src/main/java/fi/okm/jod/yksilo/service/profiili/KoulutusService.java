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
import fi.okm.jod.yksilo.dto.KoulutusDto;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.repository.KoulutusRepository;
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
public class KoulutusService {
  private final KoulutusRepository koulutukset;
  private final YksiloRepository yksilot;

  public List<KoulutusDto> findAll(JodUser user) {
    return koulutukset.findByYksilo(yksilot.getReferenceById(user.getId())).stream()
        .map(Mapper::mapKoulutus)
        .toList();
  }

  public UUID add(JodUser user, KoulutusDto dto) {
    var entity = new Koulutus(yksilot.getReferenceById(user.getId()));
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    entity.setAlkuPvm(dto.alkuPvm());
    entity.setLoppuPvm(dto.loppuPvm());
    return koulutukset.save(entity).getId();
  }

  public KoulutusDto get(JodUser user, UUID id) {
    return koulutukset
        .findByYksiloAndId(yksilot.getReferenceById(user.getId()), id)
        .map(Mapper::mapKoulutus)
        .orElseThrow(KoulutusService::notFound);
  }

  public void update(JodUser user, KoulutusDto dto) {
    var entity =
        koulutukset
            .findByYksiloAndId(yksilot.getReferenceById(user.getId()), dto.id())
            .orElseThrow(KoulutusService::notFound);
    entity.setNimi(dto.nimi());
    entity.setKuvaus(dto.kuvaus());
    koulutukset.save(entity);
  }

  public void delete(JodUser user, UUID id) {
    if (koulutukset.deleteByYksiloAndId(yksilot.getReferenceById(user.getId()), id) != 1) {
      throw notFound();
    }
  }

  static NotFoundException notFound() {
    return new NotFoundException("Not found");
  }
}
