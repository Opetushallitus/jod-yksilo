/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import com.google.common.collect.Streams;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.KiinnostuksetDto;
import fi.okm.jod.yksilo.entity.Ammatti;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.AmmattiRepository;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.service.profiili.ProfileLimitException.ProfileItem;
import fi.okm.jod.yksilo.validation.Limits;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
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
  public KiinnostuksetDto get(JodUser user) {
    var yksilo = getYksilo(user);

    var uris =
        Streams.concat(
                yksilo.getAmmattiKiinnostukset().stream().map(Ammatti::getUri),
                yksilo.getOsaamisKiinnostukset().stream().map(Osaaminen::getUri))
            .collect(Collectors.toSet());

    return new KiinnostuksetDto(uris, yksilo.getOsaamisKiinnostuksetVapaateksti());
  }

  public void update(JodUser user, Set<URI> kiinnostukset) {
    var yksilo = getYksilo(user);
    var osaamisetEntities = osaamiset.findByUriIn(kiinnostukset);
    var ammatitEntities = ammatit.findByUriIn(kiinnostukset);
    if ((osaamisetEntities.size() + ammatitEntities.size()) != kiinnostukset.size()) {
      throw new ServiceValidationException("Invalid kiinnostus");
    }

    if (kiinnostukset.size() > Limits.KIINNOSTUKSET) {
      throw new ProfileLimitException(ProfileItem.KIINNOSTUKSET);
    }

    yksilo.setOsaamisKiinnostukset(osaamisetEntities);
    yksilo.setAmmattiKiinnostukset(ammatitEntities);

    yksilot.save(yksilo);
    yksilo.updated();
  }

  public void updateVapaateksti(JodUser user, LocalizedString vapaateksti) {
    var yksilo = getYksilo(user);
    yksilo.setOsaamisKiinnostuksetVapaateksti(vapaateksti);
  }

  private Yksilo getYksilo(JodUser user) {
    return yksilot
        .findById(user.getId())
        .orElseThrow(() -> new NotFoundException("Profiili does not exist"));
  }
}
