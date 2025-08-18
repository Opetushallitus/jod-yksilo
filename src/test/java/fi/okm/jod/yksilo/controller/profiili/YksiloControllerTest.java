/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.config.mocklogin.MockJodUserImpl;
import fi.okm.jod.yksilo.dto.profiili.YksiloDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.profiili.YksiloService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = YksiloController.class)
@Import({ErrorInfoFactory.class})
@EnableWebSecurity
@Execution(ExecutionMode.SAME_THREAD)
class YksiloControllerTest {

  @MockitoBean private YksiloService yksiloService;
  @Autowired private MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @TestConfiguration
  static class TestConfig {
    @Bean
    UserDetailsService mockUserDetailsService() {
      return username -> new MockJodUserImpl(username, UUID.randomUUID());
    }
  }

  @Test
  @WithUserDetails("test")
  void shouldReturnUserInformation() throws Exception {
    when(yksiloService.get(any())).thenReturn(createYksiloDto());
    mockMvc
        .perform(get("/api/profiili/yksilo").with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.etunimi").isNotEmpty())
        .andExpect(jsonPath("$.csrf.token").isNotEmpty())
        .andExpect(jsonPath("$.tervetuloapolku").value(true));
  }

  @Test
  void shouldNotReturnCsrfTokenIfUnauthenticated() throws Exception {
    mockMvc
        .perform(get("/api/profiili/yksilo").with(csrf()))
        .andExpect(jsonPath("$.csrf.token").doesNotExist());
  }

  @Test
  @WithUserDetails("test")
  void shouldUpdateUserInformation() throws Exception {

    mockMvc
        .perform(
            put("/api/profiili/yksilo")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createYksiloDto())))
        .andExpect(status().isOk());
  }

  private static YksiloDto createYksiloDto() {
    return new YksiloDto(null, true, false, false, false, null, null, null, null, null);
  }
}
