/*
 * Copyright (c) 2024 Finnish Ministry of Education and Culture,
 * Finnish Ministry of Economic Affairs and Employment.
 * Licensed under the EUPL-1.2-or-later.
 *
 * This file is part of jod-yksilo.
 */

package fi.okm.jod.yksilo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PingController.class)
@Import(SecurityConfig.class)
class PingControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldReturnPong() throws Exception {
    mockMvc
        .perform(get("/api/v1/ping"))
        .andExpect(status().isOk())
        .andExpect(content().string("pong"));
  }

  @Test
  void shouldFailWithForbidden() throws Exception {
    mockMvc.perform(get("/api/v1/pong")).andExpect(status().isForbidden());
  }
}
