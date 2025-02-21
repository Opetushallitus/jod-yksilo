/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.errorhandler;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micrometer.tracing.Span;
import java.util.List;

public record ErrorInfo(
    ErrorCode errorCode,
    @JsonInclude(JsonInclude.Include.NON_NULL) String traceId,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<String> errorDetails) {

  ErrorInfo(ErrorCode errorCode, Span span, List<String> errorDetails) {
    this(errorCode, span == null ? null : span.context().traceId(), errorDetails);
  }

  public enum ErrorCode {
    ACCESS_DENIED,
    AUTHENTICATION_FAILURE,
    INVALID_REQUEST,
    MESSAGE_NOT_READABLE,
    RESOURCE_NOT_FOUND,
    UNSPECIFIED_ERROR,
    VALIDATION_FAILURE,
    SERVICE_ERROR,
    PERMISSION_REQUIRED, // User need to give permission.
    WRONG_PERSON,
    DATA_NOT_FOUND,
  }
}
