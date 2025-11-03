/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import fi.okm.jod.yksilo.config.feature.Feature;
import fi.okm.jod.yksilo.config.feature.FeatureRequired;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.service.inference.InferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/keskustelut")
@Slf4j
@Tag(name = "keskustelut", description = "Keskustelut")
@FeatureRequired(Feature.VIRTUAALIOHJAAJA)
public class KeskusteluController {

  private static final String CONVERSATION_SESSION_ATTRIBUTE =
      KeskusteluController.class.getName() + ".CONVERSATION";
  public static final int MAX_CONVERSATION_SECONDS = 1800;

  private final InferenceService<InferenceRequest, InferenceResponse> inferenceService;
  private final String endpoint;

  public KeskusteluController(
      InferenceService<InferenceRequest, InferenceResponse> inferenceService,
      @Value("${jod.keskustelu.endpoint}") String endpoint) {
    this.inferenceService = inferenceService;
    this.endpoint = endpoint;
  }

  @PostMapping
  @Operation(summary = "Creates a keskustelu")
  public Keskustelu createKeskustelu(
      @RequestBody @NotNull @Size(min = 2, max = 10_000) LocalizedString kuvaus,
      HttpServletRequest request) {
    if (kuvaus.asMap().size() > 1) {
      throw new IllegalArgumentException("Ambiguous request, only one language version allowed");
    }

    var entry = kuvaus.asMap().entrySet().iterator().next();
    var inferenceResponse =
        inferenceService.infer(
            endpoint,
            new InferenceRequest(null, entry.getValue(), entry.getKey().toString()),
            new ParameterizedTypeReference<>() {});

    var httpSession = request.getSession();
    httpSession.setAttribute(CONVERSATION_SESSION_ATTRIBUTE, inferenceResponse.session());

    log.info("Created a new conversation with id {}", inferenceResponse.session().id());

    return new Keskustelu(
        inferenceResponse.session().id(),
        inferenceResponse.kiinnostukset(),
        inferenceResponse.response());
  }

  @PostMapping("/{id}")
  @Operation(summary = "Continues a keskustelu")
  public ResponseEntity<Vastaus> continueKeskustelu(
      @PathVariable UUID id,
      @RequestBody @NotNull @Size(min = 2, max = 10_000) LocalizedString kuvaus,
      HttpServletRequest request) {

    if (!(request.getSession(false) instanceof HttpSession httpSession
        && httpSession.getAttribute(CONVERSATION_SESSION_ATTRIBUTE)
            instanceof InferenceSession inferenceSession)) {
      return ResponseEntity.notFound().build();
    }

    if (!inferenceSession.id().equals(id)
        || Instant.now()
            .isAfter(
                Instant.ofEpochSecond(inferenceSession.timestamp())
                    .plusSeconds(MAX_CONVERSATION_SECONDS))) {
      log.warn("Attempt to continue non-existing or expired conversation {}", id);
      httpSession.removeAttribute(CONVERSATION_SESSION_ATTRIBUTE);
      return ResponseEntity.notFound().build();
    }

    var entries = kuvaus.asMap().entrySet();
    if (entries.size() > 1) {
      throw new IllegalArgumentException("Ambiguous request, only one language version allowed");
    }

    log.info("Continuing conversation {}", id);
    var entry = entries.iterator().next();
    var inferenceResponse =
        inferenceService.infer(
            endpoint,
            new InferenceRequest(inferenceSession, entry.getValue(), entry.getKey().toString()),
            RESPONSE_TYPE);
    return ResponseEntity.ok(
        new Vastaus(inferenceResponse.kiinnostukset(), inferenceResponse.response()));
  }

  public record InferenceRequest(
      InferenceSession session,
      String message,
      @JsonProperty("language_code") String languageCode) {}

  public record InferenceResponse(
      InferenceSession session, Set<Kiinnostus> kiinnostukset, String response) {}

  public record InferenceSession(UUID id, long timestamp, String signature) {}

  public record Keskustelu(UUID id, Set<Kiinnostus> kiinnostukset, String vastaus) {}

  public record Vastaus(Set<Kiinnostus> kiinnostukset, String vastaus) {}

  public record Kiinnostus(@JsonProperty("esco_uri") URI escoUri, String kuvaus) {}

  private static final ParameterizedTypeReference<InferenceResponse> RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {};
}
