/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.onr.task;

import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;

/**
 * Loads only {@code jod.onr.*} properties from AWS SSM Parameter Store. Activated by the {@code
 * cloud} profile.
 */
@Slf4j
class SsmPropertyLoader implements EnvironmentPostProcessor {

  private static final String SSM_PREFIX = "/jod/config/";

  /** The specific ONR parameters to load. */
  private static final List<String> ONR_PARAMETERS =
      List.of(
          SSM_PREFIX + "jod.onr.base-url",
          SSM_PREFIX + "jod.onr.oid-prefix",
          SSM_PREFIX + "jod.onr.oauth2.client-id",
          SSM_PREFIX + "jod.onr.oauth2.client-secret",
          SSM_PREFIX + "jod.onr.oauth2.token-uri");

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, @NonNull SpringApplication application) {

    if (!environment.matchesProfiles("cloud")) {
      return;
    }

    log.info("Loading ONR properties from SSM Parameter Store");

    try (var ssmClient = SsmClient.create()) {
      var response =
          ssmClient.getParameters(
              GetParametersRequest.builder().names(ONR_PARAMETERS).withDecryption(true).build());

      var properties = new HashMap<String, Object>();
      for (var param : response.parameters()) {
        var propertyName = param.name().substring(SSM_PREFIX.length());
        properties.put(propertyName, param.value());
        log.debug("Loaded SSM parameter: {}", propertyName);
      }

      if (!response.invalidParameters().isEmpty()) {
        log.warn("SSM parameters not found: {}", response.invalidParameters());
      }

      log.info("Loaded {} ONR properties from SSM Parameter Store", properties.size());
      environment
          .getPropertySources()
          .addFirst(new MapPropertySource("ssmOnrProperties", properties));
    }
  }
}
