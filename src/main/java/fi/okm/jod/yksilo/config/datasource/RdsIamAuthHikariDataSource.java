/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.datasource;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

public class RdsIamAuthHikariDataSource extends HikariDataSource {

  private RdsClient rdsClient;

  @Autowired
  public void setRdsClient(RdsClient rdsClient) {
    this.rdsClient = rdsClient;
  }

  @Override
  public String getPassword() {
    // Generate URI from the JDBC URL by removing the "jdbc:" prefix.
    URI jdbcUri = URI.create(getJdbcUrl().substring(5));
    return rdsClient
        .utilities()
        .generateAuthenticationToken(
            GenerateAuthenticationTokenRequest.builder()
                .username(getUsername())
                .hostname(jdbcUri.getHost())
                .port(jdbcUri.getPort())
                .build());
  }
}
