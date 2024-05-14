/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/osaamiset")
@RequiredArgsConstructor
@Tag(name = "osaaminen", description = "Osaamisten (ESCO) listaus")
class OsaaminenController {
  private final OsaaminenRepository osaamiset;

  @GetMapping
  List<OsaaminenDto> findAll() {
    return osaamiset.findAll().stream()
        .map(it -> new OsaaminenDto(URI.create(it.getUri()), it.getNimi(), it.getKuvaus()))
        .toList();
  }
}
