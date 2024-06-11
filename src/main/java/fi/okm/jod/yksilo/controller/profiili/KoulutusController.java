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
import fi.okm.jod.yksilo.dto.profiili.KategoriaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKategoriaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusUpdateResultDto;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiili")
@RequiredArgsConstructor
@Tag(name = "profiili")
class KoulutusController {
  private final KoulutusService service;

  @GetMapping(path = "/koulutukset")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Get all koutukset and kategoriat of the user",
      description =
          """
              This endpoint can be used to get all koultukset and kategoriat of the user.
              """)
  List<KoulutusKategoriaDto> find(@AuthenticationPrincipal JodUser user) {
    return service.findAll(user);
  }

  @PostMapping(path = "/koulutukset")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Adds a new set of koulutus and its associated kategoria",
      description =
          """
              This endpoint can be used to add a new Kategoria and its associated Koulutus.
              If kategoria is not present koulutus will be added without kategoria.
              """)
  KoulutusUpdateResultDto create(
      @RequestBody @Valid KoulutusKategoriaDto dto, @AuthenticationPrincipal JodUser user) {
    return service.create(user, dto.kategoria(), dto.koulutukset());
  }

  @PatchMapping(path = "/koulutukset/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Updates the koulutus by id",
      description =
          """
              This endpoint can be used to update Koulutus.
              """)
  KoulutusUpdateResultDto updateKoulutus(
      @RequestBody @Valid KoulutusDto dto,
      @AuthenticationPrincipal JodUser user,
      @PathVariable String koulutusId) {
    if (UUID.fromString(koulutusId) != dto.id()) {
      throw new ServiceValidationException("path variable and dto id must match");
    }
    return service.update(user, dto);
  }

  @PatchMapping(path = "/kategoriat/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Updates a Kategoria by id",
      description =
          """
              This endpoint can be used to update the kagoria by id.
              """)
  KoulutusUpdateResultDto updateKategoria(
      @RequestBody @Valid KategoriaDto dto,
      @AuthenticationPrincipal JodUser user,
      @PathVariable String kategoriaId) {
    if (UUID.fromString(kategoriaId) != dto.id()) {
      throw new ServiceValidationException("path variable and dto id must match");
    }
    return service.update(user, dto);
  }

  @GetMapping("/koulutukset/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  KoulutusKategoriaDto get(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    return service.find(user, id);
  }

  @DeleteMapping("/kategoriat/{id}")
  @Operation(
      summary = "Delete the kategoria by id",
      description = """
      This endpoint can be used to delete the kategoria by id
      """)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  void delete(@PathVariable UUID id, @AuthenticationPrincipal JodUser user) {
    service.delete(user, Set.of(id));
  }

  @GetMapping("/kategoriat")
  List<KategoriaDto> getKategoriat(@AuthenticationPrincipal JodUser user) {
    return service.getKategoriat(user);
  }
}
