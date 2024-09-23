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
import fi.okm.jod.yksilo.domain.MuuOsaaminen;
import fi.okm.jod.yksilo.domain.OsaamisenLahdeTyyppi;
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

  private final YksilonOsaaminenRepository repository;
  private final YksiloRepository yksilot;
  private final YksilonOsaaminenService yksilonOsaaminenService;

  public Set<URI> findAll(JodUser user) {
    return yksilonOsaaminenService.findAll(user, OsaamisenLahdeTyyppi.MUU_OSAAMINEN, null).stream()
        .map(yo -> yo.osaaminen().uri())
        .collect(Collectors.toSet());
  }

  public void update(JodUser user, Set<URI> ids) {
    yksilonOsaaminenService.update(
        new MuuOsaaminen(
            yksilot.getReferenceById(user.getId()),
            new HashSet<>(
                repository.findAllByYksiloIdAndLahde(
                    user.getId(), OsaamisenLahdeTyyppi.MUU_OSAAMINEN, Sort.unsorted()))),
        ids);
  }
}
