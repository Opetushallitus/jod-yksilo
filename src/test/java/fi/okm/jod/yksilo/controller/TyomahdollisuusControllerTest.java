/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.TyomahdollisuusDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.TyomahdollisuusService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@WebMvcTest(value = TyomahdollisuusController.class)
@Import({ErrorInfoFactory.class, MappingConfig.class})
@Execution(ExecutionMode.SAME_THREAD)
class TyomahdollisuusControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TyomahdollisuusService service;

  @Test
  @WithMockUser
  void shouldFindDataByIds() throws Exception {

    var mockIds = Set.of(UUID.randomUUID(), UUID.randomUUID());
    var otsikko =
        new LocalizedString(
            Map.of(
                Kieli.FI, "otsikko",
                Kieli.EN, "title",
                Kieli.SV, "titel"));
    var tiivistelma =
        new LocalizedString(
            Map.of(
                Kieli.FI, "Tiivistelmä",
                Kieli.EN, "Summary",
                Kieli.SV, "Sammanfattning"));
    var kuvaus =
        new LocalizedString(
            Map.of(
                Kieli.FI, "Kuvaus",
                Kieli.EN, "Description",
                Kieli.SV, "Beskrivning"));
    var mockTyomahdolliuudet =
        List.of(
            new TyomahdollisuusDto(
                mockIds.stream().findFirst().orElse(UUID.randomUUID()),
                otsikko,
                tiivistelma,
                kuvaus,
                null,
                true));
    when(service.findByIds(mockIds)).thenReturn(mockTyomahdolliuudet);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    mockIds.forEach(uuid -> params.add("id", uuid.toString()));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/tyomahdollisuudet")
                .with(csrf())
                .params(params)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.maara", is(1)))
        .andExpect(jsonPath("$.sivuja", is(1)))
        .andExpect(jsonPath("$.sisalto", hasSize(1)))
        .andExpect(
            jsonPath("$.sisalto[0].id", oneOf(mockIds.stream().map(UUID::toString).toArray())))
        .andExpect(jsonPath("$.sisalto[0].otsikko.fi", is("otsikko")))
        .andExpect(jsonPath("$.sisalto[0].tiivistelma.fi", is("Tiivistelmä")))
        .andExpect(jsonPath("$.sisalto[0].kuvaus.fi", is("Kuvaus")))
        .andExpect(jsonPath("$.sisalto[0].otsikko.en", is("title")))
        .andExpect(jsonPath("$.sisalto[0].tiivistelma.en", is("Summary")))
        .andExpect(jsonPath("$.sisalto[0].kuvaus.en", is("Description")));
  }
}
