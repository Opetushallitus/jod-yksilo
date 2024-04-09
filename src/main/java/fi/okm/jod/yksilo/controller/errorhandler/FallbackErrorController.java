/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.errorhandler;

import fi.okm.jod.yksilo.controller.errorhandler.ErrorInfo.ErrorCode;
import io.micrometer.tracing.Tracer;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fallback error controller for errors that are not handled elsewhere. */
@RestController
@RequiredArgsConstructor
@Hidden
@Slf4j
public class FallbackErrorController implements ErrorController {
  private final Tracer tracer;

  /** Renders errors as JSON. */
  @SuppressWarnings("java:S6857")
  @RequestMapping(path = "${server.error.path:/error}")
  public ResponseEntity<ErrorInfo> error(HttpServletRequest request) {

    var status = getStatus(request);
    var exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
    log.error("Request failed with status {}", status, exception);

    return ResponseEntity.status(status)
        .body(
            new ErrorInfo(
                ErrorCode.UNSPECIFIED_ERROR, tracer.currentSpan(), List.of(status.name())));
  }

  private HttpStatus getStatus(HttpServletRequest request) {
    Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    if (statusCode == null) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    try {
      return HttpStatus.valueOf(statusCode);
    } catch (Exception ex) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
  }
}
