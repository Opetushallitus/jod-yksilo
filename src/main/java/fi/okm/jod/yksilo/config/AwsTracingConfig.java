/*
 * Copyright (c) 2024 Finnish Ministry of Education and Culture,
 * Finnish Ministry of Economic Affairs and Employment.
 * Licensed under the EUPL-1.2-or-later.
 *
 * This file is part of jod-yksilo.
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
