/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.datasource;

import java.util.Objects;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import software.amazon.awssdk.services.rds.RdsClient;

@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
@Slf4j
public class FlywayDataSourceConfig {

  /** Configures Flyway to use IAM authentication with RDS. */
  @Bean
  @ConditionalOnBean(RdsClient.class)
  public FlywayConfigurationCustomizer flywayConfigurationCustomizer(
      RdsIamAuthTokenProvider authTokenProvider) {
    return configuration -> {
      var flywayDataSource =
          DataSourceBuilder.derivedFrom(configuration.getDataSource())
              .type(SimpleIamAuthDriverDataSource.class)
              .build();
      flywayDataSource.setAuthTokenProvider(authTokenProvider);
      configuration.dataSource(flywayDataSource);
    };
  }

  @Setter
  static class SimpleIamAuthDriverDataSource extends SimpleDriverDataSource {
    private RdsIamAuthTokenProvider authTokenProvider;

    @Override
    public String getPassword() {
      return authTokenProvider.generateAuthToken(Objects.requireNonNull(getUrl()), getUsername());
    }
  }
}
