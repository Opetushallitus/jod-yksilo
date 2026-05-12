/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.onr.task;

import static java.util.Objects.requireNonNull;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.Credentials;
import java.net.URI;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.rds.model.GenerateAuthenticationTokenRequest;

@Configuration(proxyBeanMethods = false)
@Slf4j
class DataSourceConfiguration {

  @Bean
  @ConditionalOnExpression("'${spring.datasource.password:}' == ''")
  DataSource iamDataSource(Environment env) {
    log.info("Configuring datasource with RDS IAM authentication");
    var url = requireNonNull(env.getProperty("spring.datasource.url"), "Datasource URL required");
    var username = env.getProperty("spring.datasource.username");
    var rdsUtilities = RdsClient.create().utilities();

    var dataSource =
        DataSourceBuilder.create()
            .type(RdsIamAuthHikariDataSource.class)
            .url(url)
            .username(username)
            .build();
    dataSource.setRdsUtilities(rdsUtilities);
    return dataSource;
  }

  static class RdsIamAuthHikariDataSource extends HikariDataSource {

    private RdsUtilities rdsUtilities;

    void setRdsUtilities(RdsUtilities rdsUtilities) {
      this.rdsUtilities = rdsUtilities;
    }

    @Override
    public Credentials getCredentials() {
      return Credentials.of(getUsername(), getPassword());
    }

    @Override
    public String getPassword() {
      String jdbcUrl = getJdbcUrl();
      if (!requireNonNull(jdbcUrl).startsWith("jdbc:")) {
        throw new IllegalArgumentException("Invalid JDBC URL");
      }
      URI jdbcUri = URI.create(jdbcUrl.substring(5));
      return rdsUtilities.generateAuthenticationToken(
          GenerateAuthenticationTokenRequest.builder()
              .username(getUsername())
              .hostname(jdbcUri.getHost())
              .port(jdbcUri.getPort())
              .build());
    }
  }
}
