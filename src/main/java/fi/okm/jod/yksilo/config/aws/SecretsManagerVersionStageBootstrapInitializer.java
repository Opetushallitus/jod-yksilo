/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.aws;

import io.awspring.cloud.autoconfigure.config.secretsmanager.SecretsManagerClientCustomizer;
import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.bootstrap.BootstrapRegistryInitializer;

/**
 * Registers {@link SecretsManagerVersionStageInterceptor} with the Secrets Manager client used by
 * Spring Cloud AWS.
 *
 * <p>This has to happen through the {@link BootstrapRegistry}: {@code
 * SecretsManagerConfigDataLocationResolver} builds its client while the config data is being
 * loaded, long before the application context exists, and looks up the customizer from the
 * bootstrap context. A regular {@code @Bean} would never be seen. The client created here is
 * promoted to an application context bean afterward, so the interceptor applies to the runtime
 * client (e.g. config reload) as well.
 */
public class SecretsManagerVersionStageBootstrapInitializer
    implements BootstrapRegistryInitializer {

  @Override
  public void initialize(BootstrapRegistry registry) {
    registry.register(
        SecretsManagerClientCustomizer.class,
        BootstrapRegistry.InstanceSupplier.of(
            builder ->
                builder.overrideConfiguration(
                    config ->
                        config.addExecutionInterceptor(
                            new SecretsManagerVersionStageInterceptor()))));
  }
}
