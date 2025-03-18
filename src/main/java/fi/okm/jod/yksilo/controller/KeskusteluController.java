/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.service.inference.InferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/keskustelut")
@Slf4j
@Tag(name = "keskustelut", description = "Keskustelut (POC)")
public class KeskusteluController {

  private final InferenceService<Request, Response> inferenceService;
  private final String endpoint;

  public KeskusteluController(
      InferenceService<Request, Response> inferenceService,
      @Value("${jod.keskustelu.endpoint}") String endpoint) {
    this.inferenceService = inferenceService;
    this.endpoint = endpoint;
  }

  @PostMapping
  @Operation(summary = "Creates a keskustelu")
  public ResponseWithId createKeskustelu(
      @RequestBody @NotNull @Size(min = 2, max = 10_000) LocalizedString kuvaus) {
    if (kuvaus.asMap().size() > 1) {
      throw new IllegalArgumentException("Ambiguous request, only one language version allowed");
    }
    var entry = kuvaus.asMap().entrySet().iterator().next();
    var response =
        inferenceService.infer(
            endpoint,
            null,
            new Request(entry.getValue(), entry.getKey().toString()),
            new ParameterizedTypeReference<>() {});
    return new ResponseWithId(
        response.sessionId(), response.data().kiinnostukset(), response.data().response());
  }

  @PostMapping("/{id}")
  @Operation(summary = "Continues a keskustelu")
  public ResponseWithId continueKeskustelu(
      @PathVariable UUID id,
      @RequestBody @NotNull @Size(min = 2, max = 10_000) LocalizedString kuvaus) {
    if (kuvaus.asMap().size() > 1) {
      throw new IllegalArgumentException("Ambiguous request, only one language version allowed");
    }
    var entry = kuvaus.asMap().entrySet().iterator().next();
    var response =
        inferenceService.infer(
            endpoint,
            id,
            new Request(entry.getValue(), entry.getKey().toString()),
            new ParameterizedTypeReference<>() {});
    return new ResponseWithId(
        response.sessionId(), response.data().kiinnostukset(), response.data().response());
  }

  public record Request(String message, String language_code) {}

  public record Response(Set<Kiinnostus> kiinnostukset, String response) {}

  public record Kiinnostus(URI esco_uri, String kuvaus) {}

  public record ResponseWithId(UUID id, Set<Kiinnostus> kiinnostukset, String vastaus) {}
}
