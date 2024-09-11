/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.PatevyysDto;
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.profiili.ToimintoService;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = ToimintoController.class)
@Import({ErrorInfoFactory.class, MappingConfig.class})
class ToimintoControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired ObjectMapper objectMapper;

  @MockBean private ToimintoService service;

  @Test
  @WithMockUser
  void shouldFindToiminnot() throws Exception {
    mockMvc.perform(get("/api/profiili/vapaa-ajan-toiminnot")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void shouldAddToiminto() throws Exception {
    var dto =
        new ToimintoDto(
            null,
            new LocalizedString(Map.of(Kieli.FI, "testi")),
            Set.of(
                new PatevyysDto(
                    null,
                    ls("Toimenkuva"),
                    LocalDate.of(2023, 5, 1),
                    LocalDate.of(2024, 5, 1),
                    null)));

    mockMvc
        .perform(
            post("/api/profiili/vapaa-ajan-toiminnot")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isCreated());
  }

  @Test
  @WithMockUser
  void shouldFailToAddInvalidToiminto() throws Exception {
    var dto =
        new ToimintoDto(
            null,
            new LocalizedString(Map.of(Kieli.FI, "testi")),
            Set.of(
                new PatevyysDto(
                    null,
                    ls("Toimenkuva"),
                    LocalDate.of(2024, 5, 1),
                    LocalDate.of(2023, 5, 1),
                    null)));
    mockMvc
        .perform(
            post("/api/profiili/vapaa-ajan-toiminnot")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithMockUser
  void shouldGetToimintoById() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(get("/api/profiili/vapaa-ajan-toiminnot/{id}", id)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void shouldUpdateToiminto() throws Exception {
    UUID id = UUID.randomUUID();

    var updatedDto =
        new ToimintoDto(
            id,
            new LocalizedString(Map.of(Kieli.FI, "updated testi")),
            Set.of(
                new PatevyysDto(
                    null,
                    ls("Updated Toimenkuva"),
                    LocalDate.of(2023, 5, 1),
                    LocalDate.of(2024, 5, 1),
                    null)));

    mockMvc
        .perform(
            put("/api/profiili/vapaa-ajan-toiminnot/{id}", id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDto)))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void shouldFailToUpdateInvalidToiminto() throws Exception {
    var updatedDto =
        new ToimintoDto(
            UUID.randomUUID(),
            new LocalizedString(Map.of(Kieli.FI, "updated testi")),
            Set.of(
                new PatevyysDto(
                    null,
                    ls("Updated Toimenkuva"),
                    LocalDate.of(2023, 5, 1),
                    LocalDate.of(2024, 5, 1),
                    null)));

    mockMvc
        .perform(
            put("/api/profiili/toiminnot/{id}", UUID.randomUUID())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDto)))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithMockUser
  void shouldDeleteToiminnot() throws Exception {
    Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());

    mockMvc
        .perform(
            delete("/api/profiili/vapaa-ajan-toiminnot")
                .with(csrf())
                .param("ids", ids.stream().map(UUID::toString).toArray(String[]::new)))
        .andExpect(status().isNoContent());
  }
}
