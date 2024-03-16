/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import fi.okm.jod.yksilo.entity.DemoEntity;
import fi.okm.jod.yksilo.service.DemoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mock controller (to be removed). */
@RestController
@RequestMapping(path = "/api/v1/demo")
public class DemoController {

  private final DemoService demoService;

  public DemoController(DemoService demoService) {
    this.demoService = demoService;
  }

  @GetMapping
  public Iterable<DemoEntity> findAll() {
    return demoService.findAll();
  }
}
