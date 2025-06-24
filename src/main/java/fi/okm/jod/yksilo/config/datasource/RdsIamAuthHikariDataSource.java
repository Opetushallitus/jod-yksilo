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
import com.zaxxer.hikari.util.Credentials;

public class RdsIamAuthHikariDataSource extends HikariDataSource {

  private RdsIamAuthTokenProvider rdsAuthTokenProvider;

  void setAuthTokenProvider(RdsIamAuthTokenProvider rdsAuthTokenProvider) {
    this.rdsAuthTokenProvider = rdsAuthTokenProvider;
  }

  @Override
  public Credentials getCredentials() {
    return Credentials.of(getUsername(), getPassword());
  }

  @Override
  public String getPassword() {
    return rdsAuthTokenProvider.generateAuthToken(getJdbcUrl(), getUsername());
  }
}
