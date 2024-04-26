/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.errorhandler;

import static net.logstash.logback.argument.StructuredArguments.kv;

import fi.okm.jod.yksilo.errorhandler.ErrorInfo.ErrorCode;
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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fallback error controller for errors that are not handled elsewhere. */
@RestController
@RequiredArgsConstructor
@Hidden
@Slf4j
public class FallbackErrorController implements ErrorController {
  private final Tracer tracer;

  /** Renders (almost all) unhandled errors as JSON. */
  @SuppressWarnings({"java:S6857", "java:S3752"})
  @RequestMapping(path = "${server.error.path:/error}")
  public ResponseEntity<ErrorInfo> error(HttpServletRequest request) {

    HttpStatus status;
    Throwable exception;
    ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

    if (request.getAttribute(WebAttributes.ACCESS_DENIED_403) instanceof Throwable ex) {
      exception = ex;
      status = HttpStatus.FORBIDDEN;
      errorCode = ErrorCode.ACCESS_DENIED;
    } else {
      exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
      if (exception instanceof AuthenticationException) {
        status = HttpStatus.FORBIDDEN;
        errorCode = ErrorCode.AUTHENTICATION_FAILURE;
      } else {
        status = getStatus(request);
      }
    }

    if (status.is5xxServerError()) {
      errorCode = ErrorCode.UNSPECIFIED_ERROR;
      log.error("Request failed: {}", kv("status", status.value()), exception);
    } else {
      log.warn(
          "Request failed: {}, {}",
          kv("status", status.value()),
          kv("reason", exception.toString()));
    }

    return ResponseEntity.status(status)
        .header("Cache-Control", "private, no-cache, no-store, stale-if-error=0")
        .header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
        .body(new ErrorInfo(errorCode, tracer.currentSpan(), List.of(status.name())));
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
