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
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.sagemakerruntime.SageMakerRuntimeClient;

@Configuration
@Profile("cloud")
public class AwsConfig {

  @Bean
  public AwsCredentialsProvider awsCredentialsProvider() {
    return DefaultCredentialsProvider.create();
  }

  @Bean
  public Region region() {
    return new DefaultAwsRegionProviderChain().getRegion();
  }

  @Bean
  public RdsClient rdsClient(Region region) {
    return RdsClient.builder().credentialsProvider(awsCredentialsProvider()).region(region).build();
  }

  @Bean
  public SageMakerRuntimeClient sageMakerRuntimeClient(Region region) {
    return SageMakerRuntimeClient.builder()
        .credentialsProvider(awsCredentialsProvider())
        .region(region)
        .build();
  }
}
