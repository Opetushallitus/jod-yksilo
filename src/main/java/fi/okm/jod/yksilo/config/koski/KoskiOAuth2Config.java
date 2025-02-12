/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.koski;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Getter
@Slf4j
@ConditionalOnProperty(name = "jod.koski.enabled", havingValue = "true")
@Configuration(proxyBeanMethods = false)
public class KoskiOAuth2Config {

  private static final String REGISTRATION_ID = "koski";

  private final String resourceServer;

  public KoskiOAuth2Config(@Value("${jod.koski.resource-server.url}") String resourceServer) {
    this.resourceServer = resourceServer;
  }

  public String getRegistrationId() {
    return REGISTRATION_ID;
  }
}
