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
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.service.inference.InferenceService;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.Map.Entry;
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
  public Keskustelu createKeskustelu(
      @RequestBody @NotNull @Valid UusiKeskustelu uusiKeskustelu, HttpServletRequest request) {

    var inferenceResponse =
        inferenceService.infer(
            endpoint,
            InferenceRequest.newConversation(uusiKeskustelu.viesti(), uusiKeskustelu.tila()),
            new ParameterizedTypeReference<>() {});

    var httpSession = request.getSession();
    httpSession.setAttribute(CONVERSATION_SESSION_ATTRIBUTE, inferenceResponse.session());

    log.info("Created a new conversation with id {}", inferenceResponse.session().id());

    return new Keskustelu(
        inferenceResponse.session().id(),
        inferenceResponse.response(),
        inferenceResponse.suggestions());
  }

  @PostMapping("/{id}")
  public ResponseEntity<Vastaus> continueKeskustelu(
      @PathVariable UUID id,
      @RequestBody @NotNull @Size(min = 2, max = 1000) LocalizedString viesti,
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

    log.info("Continuing conversation {}", id);
    var inferenceResponse =
        inferenceService.infer(
            endpoint,
            InferenceRequest.continueConversation(inferenceSession, viesti),
            RESPONSE_TYPE);
    return ResponseEntity.ok(
        new Vastaus(inferenceResponse.response(), inferenceResponse.suggestions()));
  }

  public enum Mode {
    INTERESTS,
    SKILLS
  }

  public enum Tila {
    KIINNOSTUKSET,
    OSAAMINEN
  }

  public record InferenceRequest(
      InferenceSession session,
      String message,
      @JsonProperty("language_code") String languageCode,
      Mode mode) {

    static InferenceRequest newConversation(LocalizedString message, Tila tila) {
      var entry = validate(message);
      return new InferenceRequest(
          null,
          entry.getValue(),
          entry.getKey().getKoodi(),
          Tila.OSAAMINEN.equals(tila) ? Mode.SKILLS : Mode.INTERESTS);
    }

    static InferenceRequest continueConversation(
        InferenceSession session, LocalizedString message) {
      var entry = validate(message);
      return new InferenceRequest(session, entry.getValue(), entry.getKey().getKoodi(), null);
    }

    private static Entry<Kieli, String> validate(LocalizedString message) {
      var entries = message.asMap().entrySet();
      if (entries.size() > 1) {
        throw new IllegalArgumentException("Ambiguous request, only one language version allowed");
      }
      return entries.iterator().next();
    }
  }

  public record InferenceResponse(
      InferenceSession session, Set<URI> suggestions, String response) {}

  public record InferenceSession(UUID id, long timestamp, String signature) {}

  public record UusiKeskustelu(
      @NotNull @Size(min = 2, max = 1000) LocalizedString viesti, @NotNull Tila tila) {}

  public record Keskustelu(
      @Schema(accessMode = AccessMode.READ_ONLY) UUID id,
      @NotNull String vastaus,
      Set<URI> ehdotukset) {}

  public record Vastaus(
      @Schema(accessMode = AccessMode.READ_ONLY) @NotNull String vastaus, Set<URI> ehdotukset) {}

  private static final ParameterizedTypeReference<InferenceResponse> RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {};
}
