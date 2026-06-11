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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.TeemaDto;
import fi.okm.jod.yksilo.dto.profiili.ToimintoDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.profiili.TeemaService;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(value = TeemaController.class)
@Import({ErrorInfoFactory.class, MappingConfig.class})
class TeemaControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired ObjectMapper objectMapper;

  @MockitoBean private TeemaService service;

  @Test
  @WithMockUser
  void shouldFindTeemat() throws Exception {
    mockMvc.perform(get("/api/profiili/vapaa-ajan-teemat")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void shouldAddTeema() throws Exception {
    var dto =
        new TeemaDto(
            null,
            new LocalizedString(Map.of(Kieli.FI, "testi")),
            null,
            Set.of(
                new ToimintoDto(
                    null,
                    ls("Toiminto"),
                    ls("Kuvaus"),
                    LocalDate.of(2023, 5, 1),
                    LocalDate.of(2024, 5, 1),
                    null)));

    mockMvc
        .perform(
            post("/api/profiili/vapaa-ajan-teemat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isCreated());
  }

  @Test
  @WithMockUser
  void shouldFailToAddInvalidTeema() throws Exception {
    var dto =
        new TeemaDto(
            null,
            new LocalizedString(Map.of(Kieli.FI, "testi")),
            null,
            Set.of(
                new ToimintoDto(
                    null,
                    ls("Toiminto"),
                    ls("Kuvaus"),
                    LocalDate.of(2024, 5, 1),
                    LocalDate.of(2023, 5, 1),
                    null)));
    mockMvc
        .perform(
            post("/api/profiili/vapaa-ajan-teemat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithMockUser
  void shouldGetTeemaById() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(get("/api/profiili/vapaa-ajan-teemat/{id}", id)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser
  void shouldUpdateTeema() throws Exception {
    UUID id = UUID.randomUUID();

    var updatedDto =
        new TeemaDto(
            id,
            new LocalizedString(Map.of(Kieli.FI, "updated testi")),
            null,
            Set.of(
                new ToimintoDto(
                    null,
                    ls("Updated Toiminto"),
                    ls("Updated Kuvaus"),
                    LocalDate.of(2023, 5, 1),
                    LocalDate.of(2024, 5, 1),
                    null)));

    mockMvc
        .perform(
            put("/api/profiili/vapaa-ajan-teemat/{id}", id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDto)))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser
  void shouldFailToUpdateInvalidTeema() throws Exception {
    var updatedDto =
        new TeemaDto(
            UUID.randomUUID(),
            new LocalizedString(Map.of(Kieli.FI, "updated testi")),
            null,
            Set.of(
                new ToimintoDto(
                    null,
                    ls("Updated Toiminto"),
                    ls("Updated Kuvaus"),
                    LocalDate.of(2023, 5, 1),
                    LocalDate.of(2024, 5, 1),
                    null)));

    mockMvc
        .perform(
            put("/api/profiili/teemat/{id}", UUID.randomUUID())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedDto)))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithMockUser
  void shouldDeleteTeema() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc
        .perform(delete("/api/profiili/vapaa-ajan-teemat/{id}", id).with(csrf()))
        .andExpect(status().isNoContent());
  }
}
