/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import fi.okm.jod.yksilo.config.CvProperties;
import fi.okm.jod.yksilo.config.feature.Feature;
import fi.okm.jod.yksilo.config.feature.FeatureRequired;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.profiili.CvTehtavaDto;
import fi.okm.jod.yksilo.dto.profiili.CvTehtavaSaveDto;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import fi.okm.jod.yksilo.service.profiili.cv.CvService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiili/cv")
@Tag(name = "profiili/cv")
@RequiredArgsConstructor
@Slf4j
@FeatureRequired(Feature.CV_IMPORT)
public class CvController {

  private final CvService cvService;
  private final CvProperties properties;

  private static final byte[] MAGIC = "%PDF-".getBytes(StandardCharsets.UTF_8);
  private static final byte[] EOF = "%%EOF".getBytes(StandardCharsets.UTF_8);

  @PostMapping(consumes = "application/pdf")
  @Operation(
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content =
                  @Content(
                      mediaType = "application/pdf",
                      schema = @Schema(type = "string", format = "binary"))))
  public ResponseEntity<CvTehtavaDto> upload(
      HttpServletRequest request,
      @RequestHeader(value = HttpHeaders.CONTENT_LANGUAGE, defaultValue = "fi") Kieli lang,
      @AuthenticationPrincipal JodUser user)
      throws IOException {

    final var maxSize = properties.maxSize();
    if (request.getContentLengthLong() > maxSize) {
      return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).build();
    }

    cvService.checkNoInFlightTask(user);

    var bytes = request.getInputStream().readNBytes(maxSize + 1);
    if (bytes.length > maxSize) {
      return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).build();
    }

    // the smallest possible valid PDF is about 300 bytes
    // https://pdfa.org/the-smallest-possible-valid-pdf/
    if (bytes.length < 300 || !startsWithMagic(bytes) || !endsWithEofMarker(bytes)) {
      throw new ServiceValidationException("Invalid PDF content");
    }

    return ResponseEntity.accepted().body(cvService.submit(user, lang, bytes));
  }

  @GetMapping("/{tehtavaId}")
  public CvTehtavaDto getStatus(
      @PathVariable UUID tehtavaId, @AuthenticationPrincipal JodUser user) {
    return cvService.getStatus(user, tehtavaId);
  }

  @PostMapping("/{tehtavaId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      description =
          """
          Tallentaa valitut koulutukset/toimenkuvat/toiminnot osaamisprofiiliin.
          Onnistuneen tallennuksen jälkeen tehtävä poistetaan.
          """)
  public void save(
      @PathVariable UUID tehtavaId,
      @RequestBody @Valid CvTehtavaSaveDto dto,
      @AuthenticationPrincipal JodUser user) {
    cvService.save(user, tehtavaId, dto);
  }

  @DeleteMapping("/{tehtavaId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID tehtavaId, @AuthenticationPrincipal JodUser user) {
    cvService.delete(user, tehtavaId);
  }

  private static boolean startsWithMagic(byte[] bytes) {
    return Arrays.compare(bytes, 0, MAGIC.length, MAGIC, 0, MAGIC.length) == 0;
  }

  private static boolean endsWithEofMarker(byte[] bytes) {
    // Find %%EOF marker, allowing trailing EOL characters
    int i = bytes.length - 1;
    if (i < EOF.length - 1) {
      return false;
    }
    while (i >= EOF.length && (bytes[i] == '\n' || bytes[i] == '\r')) {
      i--;
    }
    int start = i - EOF.length + 1;
    return Arrays.compare(bytes, start, start + EOF.length, EOF, 0, EOF.length) == 0;
  }
}
