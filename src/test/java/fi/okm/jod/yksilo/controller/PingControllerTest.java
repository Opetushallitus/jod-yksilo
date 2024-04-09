/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.config.SecurityConfig;
import fi.okm.jod.yksilo.controller.errorhandler.ErrorInfoFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PingController.class)
@Import({SecurityConfig.class, ErrorInfoFactory.class})
class PingControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldReturnPong() throws Exception {
    mockMvc
        .perform(get("/api/ping"))
        .andExpect(status().isOk())
        .andExpect(content().string("pong"));
  }

  @Test
  void shouldFailWithForbidden() throws Exception {
    mockMvc.perform(get("/api/pong")).andExpect(status().isForbidden());
  }
}
