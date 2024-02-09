/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import brave.propagation.Propagation;
import brave.propagation.aws.AWSPropagation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configures trace id propagation for micrometer tracing
 *
 * <p>Picks trace id from AWS <i>X-Amzn-Trace-Id</i> header, making it possible to correlate log
 * entries with ALB requests.
 */
@Configuration
@Profile("cloud")
public class AwsTracingConfig {

  @Bean
  public Propagation.Factory awsTracePropagation() {
    return AWSPropagation.FACTORY;
  }
}
