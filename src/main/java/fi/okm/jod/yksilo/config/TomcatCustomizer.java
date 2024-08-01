/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.errorhandler.ErrorInfo;
import fi.okm.jod.yksilo.errorhandler.ErrorInfo.ErrorCode;
import fi.okm.jod.yksilo.errorhandler.TomcatErrorReportValve;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.core.StandardHost;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/** Overrides the low-level Tomcat error report valve to force JSON error responses in all cases. */
@Component
@Slf4j
class TomcatCustomizer implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

  private final String invalidRequest;
  private final String unspecifiedError;
  private final CustomTomcatProperties properties;

  TomcatCustomizer(ObjectMapper mapper, CustomTomcatProperties properties) {
    this.properties = properties;
    try {
      this.invalidRequest =
          mapper.writeValueAsString(new ErrorInfo(ErrorCode.INVALID_REQUEST, null, null));
      this.unspecifiedError =
          mapper.writeValueAsString(
              new ErrorInfo(ErrorInfo.ErrorCode.UNSPECIFIED_ERROR, null, null));
    } catch (JsonProcessingException e) {
      // SHOULD NOT HAPPEN
      throw new IllegalStateException("Mapping failed", e);
    }
  }

  @Override
  public void customize(TomcatServletWebServerFactory factory) {

    factory.addConnectorCustomizers(
        connector -> {
          if (properties.secureConnector()) {
            connector.setSecure(true);
            connector.setScheme("https");
          }
          connector.setProxyName(properties.proxyName());
          connector.setProxyPort(properties.proxyPort());
        });

    factory.addContextCustomizers(
        context -> {
          if (context.getParent() instanceof StandardHost host) {
            host.addValve(new TomcatErrorReportValve(invalidRequest, unspecifiedError));
            host.setErrorReportValveClass(TomcatErrorReportValve.class.getName());
          } else {
            log.warn("Failed to configure TomcatErrorReportValve");
          }
        });
  }

  @ConfigurationProperties("server.tomcat.custom")
  record CustomTomcatProperties(boolean secureConnector, String proxyName, int proxyPort) {}
}
