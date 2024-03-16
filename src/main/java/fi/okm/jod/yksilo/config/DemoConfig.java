/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import fi.okm.jod.yksilo.entity.DemoEntity;
import fi.okm.jod.yksilo.repository.DemoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Mock configuration (to be removed). */
@Configuration
public class DemoConfig {
  @Bean
  public CommandLineRunner commandLineRunner(DemoRepository demoRepository) {
    return (String[] args) -> {
      demoRepository.save(new DemoEntity());
      demoRepository.save(new DemoEntity());
    };
  }
}
