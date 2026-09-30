/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo;

import fi.okm.jod.yksilo.config.aws.SecretsManagerVersionStageBootstrapInitializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Application entrypoint. */
@EnableAsync
@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan("fi.okm.jod.yksilo")
public class Application {

  public static void main(String[] args) {
    new SpringApplicationBuilder(Application.class)
        .addBootstrapRegistryInitializer(new SecretsManagerVersionStageBootstrapInitializer())
        .run(args);
  }
}
