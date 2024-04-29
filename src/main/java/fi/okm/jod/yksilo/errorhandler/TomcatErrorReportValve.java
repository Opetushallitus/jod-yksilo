/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.errorhandler;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.coyote.ActionCode;

/**
 * Low-level error handler for errors not handled to the application. Used to ensure that the error
 * response is consistent.
 *
 * @see fi.okm.jod.yksilo.config.TomcatCustomizer
 * @see org.apache.catalina.valves.JsonErrorReportValve
 */
public class TomcatErrorReportValve extends ErrorReportValve {

  // Using fixed error strings to avoid unnecessary object creation (error can happen in low
  // resource conditions)
  private final String unspecifiedErrorJson;
  private final String invalidRequestJson;

  public TomcatErrorReportValve(String invalidRequestJson, String unspecifiedErrorJson) {
    super();
    this.invalidRequestJson = invalidRequestJson;
    this.unspecifiedErrorJson = unspecifiedErrorJson;
  }

  @Override
  protected void report(Request request, Response response, Throwable throwable) {

    int statusCode = response.getStatus();

    // copied from org.apache.catalina.valves.JsonErrorReportValve
    if (statusCode < 400 || response.getContentWritten() > 0 || !response.setErrorReported()) {
      return;
    }

    // copied from org.apache.catalina.valves.JsonErrorReportValve
    AtomicBoolean result = new AtomicBoolean(false);
    response.getCoyoteResponse().action(ActionCode.IS_IO_ALLOWED, result);
    if (!result.get()) {
      return;
    }

    try {
      response.setContentType("application/json");
      response.setCharacterEncoding("utf-8");
      response.setHeader("Cache-Control", "private, no-cache, no-store, stale-if-error=0");
      var writer = response.getReporter();
      if (writer != null) {
        if (statusCode == 400) {
          writer.write(invalidRequestJson);
        } else {
          writer.write(unspecifiedErrorJson);
        }
      }
    } catch (IOException | IllegalStateException e) {
      final var logger = container.getLogger();
      if (logger.isDebugEnabled()) {
        logger.debug("Writing error response failed", e);
      }
    } finally {
      try {
        response.finishResponse();
      } catch (IOException e) {
        // Ignore
      }
    }
  }
}
