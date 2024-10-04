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
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.repository.YksilonSuosikkiRepository;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class YksilonSuosikkiService {
  private final YksilonSuosikkiRepository repository;
  private final YksiloRepository yksilot;
  private final TyomahdollisuusRepository tyomahdollisuudet;

  public List<SuosikkiDto> findAll(JodUser user, @Nullable SuosikkiTyyppi tyyppi) {
    var yksilo = yksilot.getReferenceById(user.getId());
    return (tyyppi == null
            ? repository.findYksilonSuosikkiByYksilo(yksilo)
            : repository.findYksilonSuosikkiByYksiloAndTyyppi(yksilo, tyyppi))
        .stream()
            .map(
                ys ->
                    new SuosikkiDto(ys.getId(), ys.getKohdeId(), ys.getTyyppi(), ys.getCreatedAt()))
            .toList();
  }

  public void delete(JodUser user, UUID id) {
    repository.deleteYksilonSuosikkiByYksiloAndId(yksilot.getReferenceById(user.getId()), id);
  }

  public UUID create(JodUser user, UUID suosionKohdeId, SuosikkiTyyppi tyyppi) {
    return repository
        .findYksilonSuosikkiByYksiloIdAndTyomahdollisuusId(user.getId(), suosionKohdeId)
        .map(YksilonSuosikki::getId)
        .orElseGet(
            () ->
                repository
                    .save(
                        new YksilonSuosikki(
                            yksilot.getReferenceById(user.getId()),
                            tyomahdollisuudet.getReferenceById(suosionKohdeId)))
                    .getId());
  }
}
