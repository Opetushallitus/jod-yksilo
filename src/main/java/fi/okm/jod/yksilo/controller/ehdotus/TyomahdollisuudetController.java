/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.ehdotus;

import fi.okm.jod.yksilo.controller.ehdotus.TyomahdollisuudetController.Request.Data;
import fi.okm.jod.yksilo.dto.NormalizedString;
import fi.okm.jod.yksilo.service.inference.InferenceService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mock controller */
@RestController
@RequestMapping(path = "/api/ehdotus/tyomahdollisuudet")
@Slf4j
@Tag(name = "ehdotus")
public class TyomahdollisuudetController {

  private final InferenceService<Request, Object> inferenceService;
  private final String endpoint;

  public TyomahdollisuudetController(
      InferenceService<Request, Object> inferenceService,
      @Value("${jod.recommendation.tyomahdollisuus.endpoint}") String endpoint) {
    this.inferenceService = inferenceService;
    this.endpoint = endpoint;
  }

  @PostMapping
  @Timed
  public ResponseEntity<Object> createEhdotus(
      @RequestBody @Valid @Size(max = 1_000)
          List<@Size(min = 1, max = 1_000) NormalizedString> osaamiset) {

    log.info("Creating a suggestion for tyomahdollisuudet");

    var request = new Request(new Data(osaamiset.stream().map(NormalizedString::value).toList()));

    return ResponseEntity.ok(inferenceService.infer(endpoint, request, Object.class));
  }

  public record Request(Data data) {
    record Data(List<String> osaamiset) {}
  }
}
