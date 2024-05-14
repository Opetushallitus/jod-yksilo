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
import fi.okm.jod.yksilo.dto.KoulutusDto;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import fi.okm.jod.yksilo.dto.validationgroup.Update;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/api/profiili/koulutukset")
@RequiredArgsConstructor
class KoulutusController {
  private final KoulutusService service;

  @GetMapping
  List<KoulutusDto> getAll(@AuthenticationPrincipal JodUser user) {
    return service.findAll(user);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ResponseEntity<Void> add(
      @Validated(Add.class) @RequestBody KoulutusDto dto, @AuthenticationPrincipal JodUser user) {
    var id = service.add(user, dto);
    var location =
        ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
    return ResponseEntity.created(location).build();
  }

  @GetMapping("/{id}")
  KoulutusDto get(@AuthenticationPrincipal JodUser user, @PathVariable UUID id) {
    return service.get(user, id);
  }

  @PostMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void update(
      @PathVariable UUID id,
      @Validated(Update.class) @RequestBody KoulutusDto dto,
      @AuthenticationPrincipal JodUser user) {

    if (dto.id() == null || !id.equals(dto.id())) {
      throw new IllegalArgumentException("Invalid identifier");
    }
    service.update(user, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(tags = "profiili")
  void delete(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    service.delete(user, id);
  }
}
