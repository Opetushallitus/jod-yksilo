/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo;

import fi.okm.jod.yksilo.util.Base64ProtocolResolver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Application entrypoint. */
@SpringBootApplication
@ConfigurationPropertiesScan("fi.okm.jod.yksilo.config")
public class Application {

  public static void main(String[] args) {
    var app = new SpringApplication(Application.class);
    // see also https://github.com/spring-projects/spring-boot/issues/41433
    // makes it possible to inline resources in YAML config
    app.addInitializers(ctx -> ctx.addProtocolResolver(new Base64ProtocolResolver()));
    app.run(args);
  }
}
