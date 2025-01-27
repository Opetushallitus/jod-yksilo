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
import fi.okm.jod.yksilo.dto.profiili.PaamaaraDto;
import fi.okm.jod.yksilo.entity.Paamaara;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import fi.okm.jod.yksilo.repository.PaamaaraRepository;
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
public class PaamaaraService {

  public static final int MAX_PAAMAARA_COUNT = 10_000;

  private final YksiloRepository yksilot;
  private final PaamaaraRepository paamaarat;
  private final TyomahdollisuusRepository tyomahdollisuudet;
  private final KoulutusmahdollisuusRepository koulutusmahdollisuudet;

  @Transactional(readOnly = true)
  public List<PaamaaraDto> findAll(JodUser user) {
    var yksilo = yksilot.getReferenceById(user.getId());
    return paamaarat.findAllByYksilo(yksilo).stream().map(Mapper::mapPaamaara).toList();
  }

  public UUID add(JodUser user, PaamaaraDto dto) {
    var yksilo = yksilot.getReferenceById(user.getId());

    var paamaara =
        switch (dto.mahdollisuusTyyppi()) {
          case TYOMAHDOLLISUUS ->
              new Paamaara(
                  yksilo,
                  dto.tyyppi(),
                  tyomahdollisuudet
                      .findById(dto.mahdollisuusId())
                      .orElseThrow(() -> new NotFoundException("Tyomahdollisuus not found")),
                  dto.tavoite());
          case KOULUTUSMAHDOLLISUUS ->
              new Paamaara(
                  yksilo,
                  dto.tyyppi(),
                  koulutusmahdollisuudet
                      .findById(dto.mahdollisuusId())
                      .orElseThrow(() -> new NotFoundException("Koulutusmahdollisuus not found")),
                  dto.tavoite());
        };

    if (paamaarat.countByYksilo(yksilo) > MAX_PAAMAARA_COUNT) {
      throw new ServiceValidationException("Too many Paamaara");
    }
    return paamaarat.save(paamaara).getId();
  }

  public void delete(JodUser user, UUID id) {
    if (paamaarat.deleteByYksiloAndId(yksilot.getReferenceById(user.getId()), id) == 0) {
      throw new NotFoundException("Paamaara not found");
    }
  }

  public void update(JodUser user, PaamaaraDto dto) {
    var paamaara =
        paamaarat
            .findByYksiloAndId(yksilot.getReferenceById(user.getId()), dto.id())
            .orElseThrow(() -> new NotFoundException("Paamaara not found"));
    paamaara.setTyyppi(dto.tyyppi());
    paamaara.setTavoite(dto.tavoite());
  }
}
