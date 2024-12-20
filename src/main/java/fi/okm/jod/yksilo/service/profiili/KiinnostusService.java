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
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.AmmattiRepository;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class KiinnostusService {
  private final YksiloRepository yksilot;
  private final OsaaminenRepository osaamiset;
  private final AmmattiRepository ammatit;

  @Transactional(readOnly = true)
  public Set<URI> getOsaamiset(JodUser user) {
    return Stream.concat(
            yksilot.findAmmattiKiinnostukset(getYksilo(user)).stream(),
            yksilot.findOsaamisKiinnostukset(getYksilo(user)).stream())
        .map(URI::create)
        .collect(Collectors.toSet());
  }

  public void updateOsaamiset(JodUser user, Set<URI> kiinnostukset) {
    var yksilo = getYksilo(user);
    var osaamisetEntities =
        osaamiset.findByUriIn(kiinnostukset.stream().map(URI::toString).toList());
    var ammatitEntities = ammatit.findByUriIn(kiinnostukset.stream().map(URI::toString).toList());
    if ((osaamisetEntities.size() + ammatitEntities.size()) != kiinnostukset.size()) {
      throw new ServiceValidationException("Invalid kiinnostus");
    }
    yksilo.setOsaamisKiinnostukset(osaamisetEntities);
    yksilo.setAmmattiKiinnostukset(ammatitEntities);
  }

  private Yksilo getYksilo(JodUser user) {
    return yksilot
        .findById(user.getId())
        .orElseThrow(() -> new NotFoundException("Profiili does not exist"));
  }
}
