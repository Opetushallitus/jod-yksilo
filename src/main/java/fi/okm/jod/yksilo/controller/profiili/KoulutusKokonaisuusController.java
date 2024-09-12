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
import fi.okm.jod.yksilo.dto.IdDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.dto.validationgroup.Add;
import fi.okm.jod.yksilo.service.profiili.KoulutusKokonaisuusService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/profiili/koulutuskokonaisuudet")
@RequiredArgsConstructor
@Tag(name = "profiili/koulutuskokonaisuudet")
class KoulutusKokonaisuusController {
  private final KoulutusKokonaisuusService service;

  @GetMapping
  List<KoulutusKokonaisuusDto> getAll(@AuthenticationPrincipal JodUser user) {
    return service.findAll(user);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ResponseEntity<IdDto<UUID>> add(
      @Validated(Add.class) @RequestBody KoulutusKokonaisuusDto dto,
      @AuthenticationPrincipal JodUser user) {
    var id = service.add(user, dto);
    var location =
        ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
    return ResponseEntity.created(location).body(new IdDto<>(id));
  }

  @GetMapping("/{id}")
  KoulutusKokonaisuusDto get(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    return service.get(user, id);
  }

  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void update(
      @PathVariable UUID id,
      @Valid @RequestBody KoulutusKokonaisuusDto dto,
      @AuthenticationPrincipal JodUser user) {

    if (dto.id() == null || !id.equals(dto.id())) {
      throw new IllegalArgumentException("Invalid identifier");
    }
    service.update(user, dto);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    service.delete(user, id);
  }
}
