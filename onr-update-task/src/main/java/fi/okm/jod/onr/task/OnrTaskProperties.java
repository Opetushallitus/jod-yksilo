/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.onr.task;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Configuration properties for the ONR API connection. */
@ConfigurationProperties(prefix = "jod.onr")
record OnrTaskProperties(
    String baseUrl, String oidPrefix, Oauth2Config oauth2, @DefaultValue RetryConfig retry) {

  record Oauth2Config(String clientId, String clientSecret, String tokenUri) {}

  record RetryConfig(
      @DefaultValue("5s") Duration initialPollDelay,
      @DefaultValue("10") int maxRetries,
      @DefaultValue("5s") Duration delay,
      @DefaultValue("60s") Duration maxDelay,
      @DefaultValue("10m") Duration timeout) {}
}
