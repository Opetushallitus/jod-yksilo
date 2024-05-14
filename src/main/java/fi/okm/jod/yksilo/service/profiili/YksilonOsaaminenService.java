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
import fi.okm.jod.yksilo.domain.OsaamisenLahde;
import fi.okm.jod.yksilo.dto.YksilonOsaaminenDto;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen_;
import fi.okm.jod.yksilo.repository.KoulutusRepository;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import fi.okm.jod.yksilo.repository.ToimenkuvaRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.repository.YksilonOsaaminenRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class YksilonOsaaminenService {

  private final YksilonOsaaminenRepository repository;
  private final OsaaminenRepository osaamiset;
  private final YksiloRepository yksilot;
  private final KoulutusRepository koulutukset;
  private final ToimenkuvaRepository toimenkuvat;

  public List<YksilonOsaaminenDto> findAll(JodUser user) {
    var sort = Sort.by(YksilonOsaaminen_.LAHDE, YksilonOsaaminen_.OSAAMINEN, YksilonOsaaminen_.ID);
    return Mapper.mapYksilonOsaaminen(repository.findAllByYksiloId(user.getId(), sort));
  }

  public UUID add(JodUser user, URI osaaminenRef, OsaamisenLahde lahde) {

    var yksilo = yksilot.getReferenceById(user.getId());
    var osaaminen =
        osaamiset
            .findByUri(osaaminenRef.toString())
            .orElseThrow(() -> new NotFoundException("Unknown osaaminen"));

    var entity = new YksilonOsaaminen(yksilo, osaaminen);

    switch (lahde.tyyppi()) {
      case TOIMENKUVA:
        entity.setToimenkuva(
            toimenkuvat
                .findByTyopaikkaYksiloAndId(yksilo, lahde.id())
                .orElseThrow(() -> new NotFoundException("Unknown Toimenkuva")));
        break;
      case KOULUTUS:
        entity.setKoulutus(
            koulutukset
                .findByYksiloAndId(yksilo, lahde.id())
                .orElseThrow(() -> new NotFoundException("Unknown Koulutus")));
        break;
      default:
        throw new ServiceException("Invalid OsaamisenLahde");
    }

    return repository.save(entity).getId();
  }

  public YksilonOsaaminenDto get(JodUser user, UUID id) {
    return repository
        .findByYksiloIdAndId(user.getId(), id)
        .map(Mapper::mapYksilonOsaaminen)
        .orElseThrow(() -> new NotFoundException("Not found"));
  }

  public void delete(JodUser user, UUID id) {
    repository.deleteByYksiloIdAndId(user.getId(), id);
  }
}
