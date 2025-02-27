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
import fi.okm.jod.yksilo.dto.profiili.YksiloDto;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class YksiloService {
  private final YksiloRepository yksilot;

  public YksiloDto get(JodUser user) {
    var yksilo = getYksilo(user);
    return new YksiloDto(yksilo.getTervetuloapolku());
  }

  public void update(JodUser user, YksiloDto dto) {
    var yksilo = getYksilo(user);
    yksilo.setTervetuloapolku(dto.tervetuloapolku());
    yksilot.save(yksilo);
  }

  public void delete(JodUser user) {
    yksilot.deleteById(user.getId());
    yksilot.removeId(user.getId());
  }

  private Yksilo getYksilo(JodUser user) {
    return yksilot
        .findById(user.getId())
        .orElseThrow(() -> new NotFoundException("Profiili does not exist"));
  }
}
