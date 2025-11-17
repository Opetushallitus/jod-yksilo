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
import fi.okm.jod.yksilo.dto.profiili.TavoiteDto;
import fi.okm.jod.yksilo.entity.Tavoite;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import fi.okm.jod.yksilo.repository.TavoiteRepository;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TavoiteService {

  public static final int MAX_PAAMAARA_COUNT = 10_000;

  private final YksiloRepository yksilot;
  private final TavoiteRepository tavoitteet;
  private final TyomahdollisuusRepository tyomahdollisuudet;
  private final KoulutusmahdollisuusRepository koulutusmahdollisuudet;

  @Transactional(readOnly = true)
  public List<TavoiteDto> findAll(JodUser user) {
    var yksilo = yksilot.getReferenceById(user.getId());
    return tavoitteet.findAllByYksilo(yksilo).stream().map(Mapper::mapTavoite).toList();
  }

  public UUID add(JodUser user, TavoiteDto dto) {
    var yksilo = yksilot.getReferenceById(user.getId());

    var tavoite =
        new Tavoite(
            yksilo,
            tyomahdollisuudet
                .findById(dto.mahdollisuusId())
                .orElseThrow(() -> new NotFoundException("Tyomahdollisuus not found")),
            dto.tavoite(),
            dto.kuvaus());
    if (tavoitteet.countByYksilo(yksilo) > MAX_PAAMAARA_COUNT) {
      throw new ServiceValidationException("Too many Tavoite");
    }

    yksilo.updated();
    return tavoitteet.save(tavoite).getId();
  }

  public void delete(JodUser user, UUID id) {
    final Yksilo yksilo = yksilot.getReferenceById(user.getId());
    if (tavoitteet.deleteByYksiloAndId(yksilot.getReferenceById(user.getId()), id) == 0) {
      throw new NotFoundException("Tavoite not found");
    }
    yksilo.updated();
    this.yksilot.save(yksilo);
  }

  public void update(JodUser user, TavoiteDto dto) {
    final Yksilo yksilo = yksilot.getReferenceById(user.getId());
    var tavoite =
        tavoitteet
            .findByYksiloAndId(yksilo, dto.id())
            .orElseThrow(() -> new NotFoundException("Tavoite not found"));
    tavoite.setTavoite(dto.tavoite());
    tavoite.setKuvaus(dto.kuvaus());
    final Tyomahdollisuus tyomahdollisuus =
        tyomahdollisuudet
            .findById(dto.mahdollisuusId())
            .orElseThrow(() -> new NotFoundException("Koulutusmahdollisuus not found"));
    tavoite.setTyomahdollisuus(tyomahdollisuus);
    yksilo.updated();
    this.tavoitteet.save(tavoite);
  }
}
