/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.errorhandler;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ErrorInfoFactory {

  private final Tracer tracer;

  public ErrorInfoFactory(Optional<Tracer> tracer) {
    this.tracer = tracer.orElse(null);
  }

  public ErrorInfo of(ErrorInfo.ErrorCode code) {
    return new ErrorInfo(code, currentSpan(), null);
  }

  public ErrorInfo of(ErrorInfo.ErrorCode code, List<String> details) {
    return new ErrorInfo(code, currentSpan(), details);
  }

  private Span currentSpan() {
    return tracer == null ? null : tracer.currentSpan();
  }
}
