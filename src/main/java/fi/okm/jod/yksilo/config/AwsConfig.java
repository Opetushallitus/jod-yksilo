/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeClient;

@Configuration
@Profile("cloud")
public class AwsConfig {

  @Bean
  public RdsClient rdsClient(
      AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider) {
    return RdsClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(regionProvider.getRegion())
        .build();
  }

  @Bean
  public SageMakerRuntimeClient sageMakerRuntimeClient(
      AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider) {
    return SageMakerRuntimeClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(regionProvider.getRegion())
        .build();
  }
}
