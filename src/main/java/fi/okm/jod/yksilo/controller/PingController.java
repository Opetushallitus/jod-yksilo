/*
 * Copyright (c) 2024 Finnish Ministry of Education and Culture,
 * Finnish Ministry of Economic Affairs and Employment.
 * Licensed under the EUPL-1.2-or-later.
 *
 * This file is part of jod-yksilo.
 */

package fi.okm.jod.yksilo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mock controller (to be removed). */
@RestController
@RequestMapping(path = "/api/v1/ping")
@Slf4j
public class PingController {

  @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
  public String ping() {
    return "pong";
  }
}
