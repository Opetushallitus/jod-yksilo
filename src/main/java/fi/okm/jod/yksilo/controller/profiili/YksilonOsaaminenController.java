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
import fi.okm.jod.yksilo.dto.profiili.YksilonOsaaminenDto;
import fi.okm.jod.yksilo.dto.profiili.YksilonOsaaminenLisaysDto;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiili/osaamiset")
@RequiredArgsConstructor
@Tag(name = "profiili", description = "Yksilön profiilin hallinta")
class YksilonOsaaminenController {
  private final YksilonOsaaminenService service;

  @GetMapping
  List<YksilonOsaaminenDto> getAll(@AuthenticationPrincipal JodUser user) {
    return service.findAll(user);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  void add(
      @RequestBody @NotEmpty @Size(max = 1000) @Valid List<YksilonOsaaminenLisaysDto> dtos,
      @AuthenticationPrincipal JodUser user) {
    service.add(user, dtos);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    service.delete(user, id);
  }
}
