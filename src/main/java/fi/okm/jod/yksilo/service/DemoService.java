/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import fi.okm.jod.yksilo.entity.DemoEntity;
import fi.okm.jod.yksilo.repository.DemoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/** Mock service (to be removed). */
@Service
@Transactional
public class DemoService {

  private final DemoRepository demoRepository;

  public DemoService(DemoRepository demoRepository) {
    this.demoRepository = demoRepository;
  }

  public Iterable<DemoEntity> findAll() {
    return demoRepository.findAll();
  }
}
