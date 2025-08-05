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
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.MuuOsaaminen;
import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.repository.YksilonOsaaminenRepository;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class MuuOsaaminenService {

  private final YksilonOsaaminenRepository osaaminen;
  private final YksiloRepository yksilot;
  private final YksilonOsaaminenService osaaminenService;

  public Set<URI> findAll(JodUser user) {
    return osaaminenService.findAll(user, OsaamisenLahdeTyyppi.MUU_OSAAMINEN, null).stream()
        .map(yo -> yo.osaaminen().uri())
        .collect(Collectors.toSet());
  }

  public void update(JodUser user, Set<URI> ids) {
    osaaminenService.updateLahteenOsaamiset(
        getMuuOsaaminen(yksilot.getReferenceById(user.getId())),
        osaaminenService.getOsaamiset(ids));
  }

  public LocalizedString getVapaateksti(JodUser user) {
    var yksilo = yksilot.getReferenceById(user.getId());
    return yksilo.getMuuOsaaminenVapaateksti();
  }

  public void updateVapaateksti(JodUser user, LocalizedString vapaateksti) {
    var yksilo = yksilot.getReferenceById(user.getId());
    yksilo.setMuuOsaaminenVapaateksti(vapaateksti);
  }

  void add(Yksilo yksilo, Set<Osaaminen> osaamiset) {
    osaaminenService.addLahteenOsaamiset(getMuuOsaaminen(yksilo), osaamiset);
  }

  private MuuOsaaminen getMuuOsaaminen(Yksilo yksilo) {
    return new MuuOsaaminen(
        yksilo,
        new HashSet<>(
            osaaminen.findAllByYksiloIdAndLahde(
                yksilo.getId(), OsaamisenLahdeTyyppi.MUU_OSAAMINEN, Sort.unsorted())));
  }
}
