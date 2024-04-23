/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.datasource;

import java.net.URI;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

@Component
@ConditionalOnBean(RdsClient.class)
public class RdsIamAuthTokenProvider {
  private final RdsUtilities rdsUtilities;

  public RdsIamAuthTokenProvider(RdsClient rdsClient) {
    this.rdsUtilities = rdsClient.utilities();
  }

  public String generateAuthToken(String jdbcUrl, String username) {
    if (!Objects.requireNonNull(jdbcUrl).startsWith("jdbc:")) {
      throw new IllegalArgumentException("Invalid JDBC URL");
    }
    URI jdbcUri = URI.create(jdbcUrl.substring(5));
    return rdsUtilities.generateAuthenticationToken(
        GenerateAuthenticationTokenRequest.builder()
            .username(username)
            .hostname(jdbcUri.getHost())
            .port(jdbcUri.getPort())
            .build());
  }
}
