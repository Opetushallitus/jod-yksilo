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
import fi.okm.jod.yksilo.domain.OsaamisenLahde;
import fi.okm.jod.yksilo.dto.YksilonOsaaminenDto;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
  ResponseEntity<Void> add(
      @Valid @RequestBody YksilonOsaaminenController.OsaaminenLisaysDto dto,
      @AuthenticationPrincipal JodUser user) {
    var id = service.add(user, dto.osaaminen(), dto.lahde());
    var location =
        ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
    return ResponseEntity.created(location).build();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    service.delete(user, id);
  }

  record OsaaminenLisaysDto(
      @NotNull @Schema(description = "Reference") URI osaaminen, @NotNull OsaamisenLahde lahde) {}
}
