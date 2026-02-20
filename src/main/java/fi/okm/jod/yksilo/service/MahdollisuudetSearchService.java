/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.MahdollisuusDto;
import fi.okm.jod.yksilo.repository.MahdollisuusRepository;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuudetService;
import java.util.List;
import java.util.SequencedCollection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MahdollisuudetSearchService {

  private final MahdollisuusRepository mahdollisuusRepository;
  private final MahdollisuudetService mahdollisuudetService;

  public SequencedCollection<MahdollisuusDto> search(Kieli lang, String query) {
    var ids = mahdollisuusRepository.searchBy(query, lang);
    if (ids.isEmpty()) {
      return List.of();
    }
    var mahdollisuudet =
        mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(Direction.ASC, lang);

    return ids.stream().map(mahdollisuudet::get).toList();
  }
}
