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
import fi.okm.jod.yksilo.domain.SuosikkiTyyppi;
import fi.okm.jod.yksilo.dto.profiili.SuosikkiDto;
import fi.okm.jod.yksilo.entity.YksilonSuosikki;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.repository.YksilonSuosikkiRepository;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class YksilonSuosikkiService {

  private final YksiloRepository yksilot;
  private final YksilonSuosikkiRepository suosikit;
  private final TyomahdollisuusRepository tyomahdollisuudet;
  private final KoulutusmahdollisuusRepository koulutusmahdollisuudet;

  @Transactional(readOnly = true)
  public List<SuosikkiDto> findAll(JodUser user, @Nullable SuosikkiTyyppi tyyppi) {
    var yksilo = yksilot.getReferenceById(user.getId());
    return (tyyppi == null
            ? suosikit.findByYksilo(yksilo)
            : suosikit.findByYksiloAndTyyppi(yksilo, tyyppi))
        .stream()
            .map(ys -> new SuosikkiDto(ys.getId(), ys.getKohdeId(), ys.getTyyppi(), ys.getLuotu()))
            .toList();
  }

  public UUID add(JodUser user, UUID kohdeId, SuosikkiTyyppi tyyppi) {
    var yksilo = yksilot.getReferenceById(user.getId());

    return suosikit
        .findBy(yksilo, tyyppi, kohdeId)
        .map(YksilonSuosikki::getId)
        .orElseGet(
            () ->
                suosikit
                    .save(
                        switch (tyyppi) {
                          case TYOMAHDOLLISUUS ->
                              new YksilonSuosikki(
                                  yksilo, require(tyomahdollisuudet.findById(kohdeId)));

                          case KOULUTUSMAHDOLLISUUS ->
                              new YksilonSuosikki(
                                  yksilo, require(koulutusmahdollisuudet.findById(kohdeId)));
                        })
                    .getId());
  }

  public void delete(JodUser user, UUID id) {
    if (suosikit.deleteByYksiloAndId(yksilot.getReferenceById(user.getId()), id) == 0) {
      throw new NotFoundException("Suosikki not found");
    }
  }

  private static <T> T require(Optional<T> entity) {
    return entity.orElseThrow(() -> new ServiceValidationException("Invalid Suosikki"));
  }
}
