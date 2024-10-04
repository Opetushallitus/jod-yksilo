/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.SuosikkiTyyppi;
import fi.okm.jod.yksilo.dto.profiili.SuosikkiDto;
import fi.okm.jod.yksilo.service.profiili.YksilonSuosikkiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiili/suosikki")
@RequiredArgsConstructor
@Tag(name = "profiili/suosikki")
class YksilonSuosikkiController {
  private final YksilonSuosikkiService service;

  @GetMapping
  @Operation(summary = "Finds all yksilon suosikit")
  List<SuosikkiDto> get(
      @AuthenticationPrincipal JodUser user,
      @RequestParam(required = false) SuosikkiTyyppi tyyppi) {
    return service.findAll(user, tyyppi);
  }

  @DeleteMapping
  @Operation(summary = "Deletes one of Yksilo's suosikki")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@RequestParam UUID id, @AuthenticationPrincipal JodUser user) {
    service.delete(user, id);
  }

  @PostMapping
  @Operation(summary = "Add a Yksilo's suosikki")
  UUID post(@AuthenticationPrincipal JodUser user, @RequestBody SuosikkiDto dto) {
    return service.create(user, dto.suosionKohdeId(), dto.tyyppi());
  }
}
