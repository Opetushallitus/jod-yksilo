/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.MahdollisuusDto;
import fi.okm.jod.yksilo.service.SearchService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/haku")
@Slf4j
@Tag(name = "haku", description = "Haku")
@RequiredArgsConstructor
public class HakuController {
  private final SearchService searchService;

  public record SearchQueryDto(@NotNull @Size(min = 2, max = 10_000) String query) {}
  ;

  @PostMapping
  @Timed
  public ResponseEntity<List<MahdollisuusDto>> search(
      @RequestHeader(value = HttpHeaders.CONTENT_LANGUAGE, defaultValue = "fi") Kieli lang,
      @RequestBody @NotNull SearchQueryDto query) {

    return ResponseEntity.ok(searchService.search(Kieli.FI, query.query()));
  }
}
