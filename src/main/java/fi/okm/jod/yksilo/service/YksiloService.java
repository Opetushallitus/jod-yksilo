/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.YksiloDto;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@RequiredArgsConstructor
public class YksiloService {
  private final YksiloRepository yksilot;
  private final KuvaService kuvat;

  public UUID addKuva(JodUser user, MultipartFile file) throws IOException {
    final var yksilo = yksilot.getReferenceById(user.getId());
    var kuva = kuvat.add(user, file);
    yksilo.setKuva(kuva);
    yksilot.save(yksilo);
    return kuva.getId();
  }

  public void deleteKuva(JodUser user) {
    final var yksilo = yksilot.getReferenceById(user.getId());
    yksilo.setKuva(null);
    yksilot.save(yksilo);
  }

  public YksiloDto findYksilo(JodUser user) {
    return yksilot
        .findById(user.getId())
        .map(YksiloService::mapYksilo)
        .orElseThrow(() -> new NotFoundException("Yksilö not found"));
  }

  public void deleteYksilo(JodUser user) {
    yksilot.deleteById(user.getId());
  }

  public static YksiloDto mapYksilo(Yksilo entity) {
    return entity == null
        ? null
        : new YksiloDto(entity.getId(), entity.getKuva() != null ? entity.getKuva().getId() : null);
  }
}
