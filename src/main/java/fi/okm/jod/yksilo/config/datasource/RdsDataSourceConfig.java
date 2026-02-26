/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.datasource;

import static java.util.Objects.requireNonNull;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.rds.RdsClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    name = "spring.datasource.type",
    havingValue = "fi.okm.jod.yksilo.config.datasource.RdsIamAuthHikariDataSource")
public class RdsDataSourceConfig {

  @Bean
  @ConfigurationProperties("spring.datasource.hikari")
  HikariDataSource dataSource(
      DataSourceProperties properties, RdsIamAuthTokenProvider rdsAuthTokenProvider) {

    var builder =
        DataSourceBuilder.create(properties.getClassLoader())
            .type(RdsIamAuthHikariDataSource.class);
    var dataSource =
        builder
            .url(requireNonNull(properties.getUrl(), "Datasource URL required"))
            .username(properties.getUsername())
            .build();
    dataSource.setAuthTokenProvider(rdsAuthTokenProvider);
    return dataSource;
  }

  @Bean
  RdsIamAuthTokenProvider rdsAuthTokenProvider(RdsClient rdsClient) {
    return new RdsIamAuthTokenProvider(rdsClient);
  }
}
