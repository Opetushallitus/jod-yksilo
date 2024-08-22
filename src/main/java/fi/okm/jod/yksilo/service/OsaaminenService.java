/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.dto.SivuDto;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class OsaaminenService {
  private final OsaaminenRepository osaamiset;

  public SivuDto<OsaaminenDto> findAll(int sivu, int koko) {
    return new SivuDto<>(
        osaamiset
            .findAll(PageRequest.of(sivu, koko, Sort.by("id")))
            .map(it -> new OsaaminenDto(URI.create(it.getUri()), it.getNimi(), it.getKuvaus())));
  }

  public SivuDto<OsaaminenDto> findBy(int sivu, int koko, Set<URI> uri) {
    return new SivuDto<>(
        osaamiset
            .findByUriIn(
                uri.stream().map(URI::toString).toList(), PageRequest.of(sivu, koko, Sort.by("id")))
            .map(it -> new OsaaminenDto(URI.create(it.getUri()), it.getNimi(), it.getKuvaus())));
  }

  public List<OsaaminenDto> findBy(Set<URI> uri) {
    return osaamiset
        .findByUriIn(uri.stream().map(URI::toString).toList(), Pageable.unpaged())
        .map(it -> new OsaaminenDto(URI.create(it.getUri()), it.getNimi(), it.getKuvaus()))
        .toList();
  }
}
