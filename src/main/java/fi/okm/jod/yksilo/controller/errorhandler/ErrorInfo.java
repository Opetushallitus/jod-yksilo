/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.errorhandler;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micrometer.tracing.Span;
import java.util.List;

public record ErrorInfo(
    ErrorCode errorCode,
    String traceId,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<String> errorDetails) {

  ErrorInfo(ErrorCode errorCode, Span span, List<String> errorDetails) {
    this(errorCode, span == null ? null : span.context().traceId(), errorDetails);
  }

  public enum ErrorCode {
    VALIDATION_FAILURE,
    ACCESS_DENIED,
    MESSAGE_NOT_READABLE,
    UNSPECIFIED_ERROR,
    RESOURCE_NOT_FOUND,
  }
}
