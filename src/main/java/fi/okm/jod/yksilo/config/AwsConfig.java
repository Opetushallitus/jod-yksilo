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
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.s3.S3Client;
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
        .overrideConfiguration(
            c -> c.retryStrategy(StandardRetryStrategy.builder().maxAttempts(4).build()))
        .build();
  }

  @Bean
  public S3Client s3Client(
      AwsCredentialsProvider credentialsProvider, AwsRegionProvider regionProvider) {
    return S3Client.builder()
        .region(Region.EU_NORTH_1)
        .credentialsProvider(credentialsProvider) // set your region
        .build();
  }
}
