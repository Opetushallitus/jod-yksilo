/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.errorhandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

class ExceptionHandlerAdviceTest {

  private ExceptionHandlerAdvice handler;
  private ErrorInfoFactory errorInfoFactory;
  private ObjectProvider<Tracer> tracerProvider;
  private Tracer tracer;
  private Span span;

  @BeforeEach
  void setUp() {
    errorInfoFactory = mock(ErrorInfoFactory.class);
    tracerProvider = mock(ObjectProvider.class);
    tracer = mock(Tracer.class);
    span = mock(Span.class);
    when(tracerProvider.getIfAvailable()).thenReturn(tracer);
    when(tracer.currentSpan()).thenReturn(span);
    handler = new ExceptionHandlerAdvice(errorInfoFactory, tracerProvider);
  }

  @Test
  void markSpanErrorFor5xxException() {
    var exception = new Exception("Server error");
    var request = mock(WebRequest.class);
    var errorInfo = mock(ErrorInfo.class);
    when(errorInfoFactory.of(any(), any())).thenReturn(errorInfo);

    handler.handleExceptionInternal(
        exception, errorInfo, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);

    verify(span).error(exception);
  }

  @Test
  void markSpanErrorFor4xxException() {
    var exception = new Exception("Bad request");
    var request = mock(WebRequest.class);
    var errorInfo = mock(ErrorInfo.class);
    when(errorInfoFactory.of(any(), any())).thenReturn(errorInfo);

    handler.handleExceptionInternal(
        exception, errorInfo, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

    verify(span).error(exception);
  }

  @Test
  void skipSpanErrorForNotFound() {
    var exception = new Exception("Not found");
    var request = mock(WebRequest.class);
    var errorInfo = mock(ErrorInfo.class);
    when(errorInfoFactory.of(any(), any())).thenReturn(errorInfo);

    handler.handleExceptionInternal(
        exception, errorInfo, new HttpHeaders(), HttpStatus.NOT_FOUND, request);

    verify(span, never()).error(any());
  }

  @Test
  void handleNullSpan() {
    when(tracer.currentSpan()).thenReturn(null);
    var exception = new Exception("Server error");
    var request = mock(WebRequest.class);
    var errorInfo = mock(ErrorInfo.class);
    when(errorInfoFactory.of(any(), any())).thenReturn(errorInfo);

    handler.handleExceptionInternal(
        exception, errorInfo, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);

    // Should not throw NPE
    verify(span, never()).error(any());
  }

  @Test
  void handleMissingTracerBean() {
    var noTracerProvider = mock(ObjectProvider.class);
    when(noTracerProvider.getIfAvailable()).thenReturn(null);
    var handlerWithoutTracer = new ExceptionHandlerAdvice(errorInfoFactory, noTracerProvider);

    var exception = new Exception("Server error");
    var request = mock(WebRequest.class);
    var errorInfo = mock(ErrorInfo.class);
    when(errorInfoFactory.of(any(), any())).thenReturn(errorInfo);

    // Should not throw NPE even when no Tracer bean is available (e.g. in tests)
    var response =
        handlerWithoutTracer.handleExceptionInternal(
            exception, errorInfo, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isEqualTo(errorInfo);
  }
}
