/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.errorhandler;

import com.fasterxml.jackson.databind.JsonMappingException;
import fi.okm.jod.yksilo.errorhandler.ErrorInfo.ErrorCode;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@SuppressWarnings("NullableProblems")
@ControllerAdvice
@Component
@RequiredArgsConstructor
@Slf4j
class ExceptionHandlerAdvice extends ResponseEntityExceptionHandler {

  private final ErrorInfoFactory errorInfo;

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    List<String> details = null;
    if (ex.getCause() instanceof JsonMappingException) {
      details = List.of("JsonMappingFailure");
    }
    var info = errorInfo.of(ErrorCode.MESSAGE_NOT_READABLE, details);
    return handleExceptionInternal(ex, info, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var details =
        ex.getAllValidationResults().stream()
            .flatMap(e -> e.getResolvableErrors().stream().map(r -> r.getDefaultMessage()))
            .toList();
    var info = errorInfo.of(ErrorCode.VALIDATION_FAILURE, details);
    return handleExceptionInternal(ex, info, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    var details = ex.getFieldErrors().stream().map(e -> e.getDefaultMessage()).toList();
    var info = errorInfo.of(ErrorCode.VALIDATION_FAILURE, details);

    return handleExceptionInternal(ex, info, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleNoResourceFoundException(
      NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    var info = errorInfo.of(ErrorCode.RESOURCE_NOT_FOUND, null);
    return handleExceptionInternal(ex, info, headers, status, request);
  }

  @ExceptionHandler(NotFoundException.class)
  protected ResponseEntity<Object> handleServiceException(ServiceException ex, WebRequest request) {
    var info = errorInfo.of(ErrorCode.RESOURCE_NOT_FOUND, List.of(ex.getMessage()));
    return handleExceptionInternal(ex, info, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
  }

  @ExceptionHandler(ServiceValidationException.class)
  protected ResponseEntity<Object> handleServiceException(
      ServiceValidationException ex, WebRequest request) {
    var info = errorInfo.of(ErrorCode.VALIDATION_FAILURE, List.of(ex.getMessage()));
    return handleExceptionInternal(ex, info, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  protected ResponseEntity<Object> handleServiceException(
      ConstraintViolationException ex, WebRequest request) {
    var info =
        errorInfo.of(
            ErrorCode.VALIDATION_FAILURE,
            ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage).toList());
    return handleExceptionInternal(ex, info, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  protected ResponseEntity<Object> handleServiceException(
      IllegalArgumentException ex, WebRequest request) {
    var info = errorInfo.of(ErrorCode.INVALID_REQUEST, List.of(ex.getMessage()));
    return handleExceptionInternal(ex, info, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

    if (status.is5xxServerError()) {
      log.error("Request failed", ex);
    } else if (!status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
      log.warn("Request failed: {}", ex.getMessage());
    }

    ErrorInfo info;
    if (body instanceof ErrorInfo e) {
      info = e;
    } else {
      info = errorInfo.of(ErrorCode.UNSPECIFIED_ERROR, List.of(status.toString()));
    }

    return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(info);
  }
}
