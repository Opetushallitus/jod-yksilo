/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.ehdotus;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.KoulutusmahdollisuusService;
import fi.okm.jod.yksilo.service.OsaaminenService;
import fi.okm.jod.yksilo.service.TyomahdollisuusService;
import fi.okm.jod.yksilo.service.inference.InferenceService;
import java.net.URI;
import java.util.Collections;
import java.util.List;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(value = MahdollisuudetController.class)
@Import({ErrorInfoFactory.class, MappingConfig.class})
class MahdollisuudetControllerTest {

  @Autowired ObjectMapper objectMapper;
  @Autowired private MockMvc mockMvc;
  @MockBean private TyomahdollisuusService tyomahdollisuusService;

  @MockBean private KoulutusmahdollisuusService koulutusmahdollisuusService;

  @MockBean private OsaaminenService osaaminenService;

  @MockBean
  private InferenceService<MahdollisuudetController.Request, MahdollisuudetController.Response>
      inferenceService;

  @Test
  @WithMockUser
  void whenEmptySkillsShouldReturnEmptySet() throws Exception {
    var luoEhdotusDto =
        new MahdollisuudetController.LuoEhdotusDto(
            0.5, Collections.emptySet(), 0.5, Collections.emptySet());

    var tyomahdollisuusIds = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    var koulutusmahdollisuusIds = Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    when(tyomahdollisuusService.fetchAllIds()).thenReturn(tyomahdollisuusIds);
    when(koulutusmahdollisuusService.fetchAllIds()).thenReturn(koulutusmahdollisuusIds);
    var response =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/ehdotus/mahdollisuudet")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(luoEhdotusDto)))
            .andExpect(status().isOk());

    response.andExpect(jsonPath("$.length()").value("6"));

    var r =
        objectMapper.readValue(
            response.andReturn().getResponse().getContentAsString(),
            new TypeReference<List<MahdollisuudetController.EhdotusDto>>() {});

    // test all työmahdollisuudet are included in response with empty metadata with type
    // TYOMAHDOLLISUUS
    assertTrue(
        r.stream()
            .filter(
                m ->
                    m.ehdotusMetadata().tyyppi()
                        == MahdollisuudetController.MahdollisuusTyyppi.TYOMAHDOLLISUUS)
            .allMatch(
                m ->
                    tyomahdollisuusIds.contains(m.mahdollisuusId())
                        && m.ehdotusMetadata().pisteet() == null
                        && m.ehdotusMetadata().trendi() == null
                        && m.ehdotusMetadata().tyollisyysNakyma() == null));

    // test all koulutusmahdollisuudet are included in response with empty metadata with type
    // KOULUTUSMAHDOLLISUUS
    assertTrue(
        r.stream()
            .filter(
                m ->
                    m.ehdotusMetadata().tyyppi()
                        == MahdollisuudetController.MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS)
            .allMatch(
                m ->
                    koulutusmahdollisuusIds.contains(m.mahdollisuusId())
                        && m.ehdotusMetadata().pisteet() == null
                        && m.ehdotusMetadata().trendi() == null
                        && m.ehdotusMetadata().tyollisyysNakyma() == null));
  }

  @Test
  @WithMockUser
  void whenSkillsShouldReturnScores() throws Exception {
    var luoEhdotusDto =
        new MahdollisuudetController.LuoEhdotusDto(
            0.5, Set.of(URI.create("http://dymmy")), 0.5, Set.of(URI.create("http://dymmy")));

    var tyomahdollisuusIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    var koulutusmahdollisuusIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    var inferenceResponse = new MahdollisuudetController.Response();
    inferenceResponse.addAll(
        List.of(
            new MahdollisuudetController.Response.Suggestion(
                tyomahdollisuusIds.get(0), 0.99d, true, false),
            new MahdollisuudetController.Response.Suggestion(
                tyomahdollisuusIds.get(1), 0.89d, true, false),
            new MahdollisuudetController.Response.Suggestion(
                tyomahdollisuusIds.get(2), 0.79d, true, false),
            new MahdollisuudetController.Response.Suggestion(
                koulutusmahdollisuusIds.get(0), 0.98d, false, true),
            new MahdollisuudetController.Response.Suggestion(
                koulutusmahdollisuusIds.get(1), 0.88d, false, true),
            new MahdollisuudetController.Response.Suggestion(
                koulutusmahdollisuusIds.get(2), 0.78d, false, true)));

    var osaamiset =
        List.of(
            new OsaaminenDto(
                URI.create("http://dummy"),
                new LocalizedString(Map.of(Kieli.FI, "text")),
                new LocalizedString(Map.of(Kieli.FI, "text"))));

    when(tyomahdollisuusService.fetchAllIds()).thenReturn(Set.copyOf(tyomahdollisuusIds));
    when(koulutusmahdollisuusService.fetchAllIds()).thenReturn(Set.copyOf(koulutusmahdollisuusIds));
    when(osaaminenService.findBy(any())).thenReturn(osaamiset);

    when(inferenceService.infer(anyString(), any(), any())).thenReturn(inferenceResponse);
    var response =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/ehdotus/mahdollisuudet")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(luoEhdotusDto)))
            .andExpect(status().isOk());

    response.andExpect(jsonPath("$.length()").value("6"));

    var r =
        objectMapper.readValue(
            response.andReturn().getResponse().getContentAsString(),
            new TypeReference<List<MahdollisuudetController.EhdotusDto>>() {});

    // test all työmahdollisuudet are included in response with empty metadata with type
    // TYOMAHDOLLISUUS
    assertTrue(
        r.stream()
            .filter(
                m ->
                    m.ehdotusMetadata().tyyppi()
                        == MahdollisuudetController.MahdollisuusTyyppi.TYOMAHDOLLISUUS)
            .allMatch(
                m ->
                    tyomahdollisuusIds.contains(m.mahdollisuusId())
                        && m.ehdotusMetadata().pisteet() != null
                        && m.ehdotusMetadata().pisteet() > 0
                        && m.ehdotusMetadata().trendi() == null
                        && m.ehdotusMetadata().tyollisyysNakyma() == null));

    // test all koulutusmahdollisuudet are included in response with empty metadata with type
    // KOULUTUSMAHDOLLISUUS
    assertTrue(
        r.stream()
            .filter(
                m ->
                    m.ehdotusMetadata().tyyppi()
                        == MahdollisuudetController.MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS)
            .allMatch(
                m ->
                    koulutusmahdollisuusIds.contains(m.mahdollisuusId())
                        && m.ehdotusMetadata().pisteet() != null
                        && m.ehdotusMetadata().pisteet() > 0
                        && m.ehdotusMetadata().trendi() == null
                        && m.ehdotusMetadata().tyollisyysNakyma() == null));
  }

  @Test
  @WithMockUser
  void ifInferenceReturnsUnknownIdShouldNotCauseNPE() throws Exception {
    var luoEhdotusDto =
        new MahdollisuudetController.LuoEhdotusDto(
            0.5, Set.of(URI.create("http://dymmy")), 0.5, Set.of(URI.create("http://dymmy")));

    var tyomahdollisuusIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    var koulutusmahdollisuusIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    var inferenceResponse = new MahdollisuudetController.Response();
    inferenceResponse.addAll(
        List.of(
            new MahdollisuudetController.Response.Suggestion(
                tyomahdollisuusIds.get(0), 0.99d, true, false),
            new MahdollisuudetController.Response.Suggestion(
                tyomahdollisuusIds.get(1), 0.89d, true, false),
            new MahdollisuudetController.Response.Suggestion(
                tyomahdollisuusIds.get(2), 0.79d, true, false),
            new MahdollisuudetController.Response.Suggestion(
                koulutusmahdollisuusIds.get(0), 0.98d, false, true),
            new MahdollisuudetController.Response.Suggestion(
                koulutusmahdollisuusIds.get(1), 0.88d, false, true),
            new MahdollisuudetController.Response.Suggestion(
                koulutusmahdollisuusIds.get(2), 0.78d, false, true),
            new MahdollisuudetController.Response.Suggestion(
                UUID.randomUUID(), 0.78d, false, true)));

    var osaamiset =
        List.of(
            new OsaaminenDto(
                URI.create("http://dummy"),
                new LocalizedString(Map.of(Kieli.FI, "text")),
                new LocalizedString(Map.of(Kieli.FI, "text"))));

    when(tyomahdollisuusService.fetchAllIds()).thenReturn(Set.copyOf(tyomahdollisuusIds));
    when(koulutusmahdollisuusService.fetchAllIds()).thenReturn(Set.copyOf(koulutusmahdollisuusIds));
    when(osaaminenService.findBy(any())).thenReturn(osaamiset);

    when(inferenceService.infer(anyString(), any(), any())).thenReturn(inferenceResponse);
    var response =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/ehdotus/mahdollisuudet")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(luoEhdotusDto)))
            .andExpect(status().isOk());

    response.andExpect(jsonPath("$.length()").value("6"));
  }

  @Test
  @WithMockUser
  void ifInferenceReturnsMissingIdShouldNotCauseNPE() throws Exception {
    var luoEhdotusDto =
        new MahdollisuudetController.LuoEhdotusDto(
            0.5, Set.of(URI.create("http://dymmy")), 0.5, Set.of(URI.create("http://dymmy")));

    var tyomahdollisuusIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    var koulutusmahdollisuusIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    var inferenceResponse = new MahdollisuudetController.Response();
    inferenceResponse.addAll(
        List.of(
            new MahdollisuudetController.Response.Suggestion(
                tyomahdollisuusIds.get(0), 0.99d, true, false),
            new MahdollisuudetController.Response.Suggestion(
                tyomahdollisuusIds.get(1), 0.89d, true, false),
            new MahdollisuudetController.Response.Suggestion(
                tyomahdollisuusIds.get(2), 0.79d, true, false),
            new MahdollisuudetController.Response.Suggestion(
                koulutusmahdollisuusIds.get(0), 0.98d, false, true),
            new MahdollisuudetController.Response.Suggestion(
                koulutusmahdollisuusIds.get(1), 0.88d, false, true)));

    var osaamiset =
        List.of(
            new OsaaminenDto(
                URI.create("http://dummy"),
                new LocalizedString(Map.of(Kieli.FI, "text")),
                new LocalizedString(Map.of(Kieli.FI, "text"))));

    when(tyomahdollisuusService.fetchAllIds()).thenReturn(Set.copyOf(tyomahdollisuusIds));
    when(koulutusmahdollisuusService.fetchAllIds()).thenReturn(Set.copyOf(koulutusmahdollisuusIds));
    when(osaaminenService.findBy(any())).thenReturn(osaamiset);

    when(inferenceService.infer(anyString(), any(), any())).thenReturn(inferenceResponse);
    var response =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/ehdotus/mahdollisuudet")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(luoEhdotusDto)))
            .andExpect(status().isOk());

    response.andExpect(jsonPath("$.length()").value("6"));

    var r =
        objectMapper.readValue(
            response.andReturn().getResponse().getContentAsString(),
            new TypeReference<List<MahdollisuudetController.EhdotusDto>>() {});
    // assert that missing koulutusmahdollisuus was returned as empty with proper type
    assertTrue(
        r.stream()
            .filter(m -> m.mahdollisuusId() == koulutusmahdollisuusIds.get(2))
            .allMatch(
                m ->
                    m.ehdotusMetadata().pisteet() == null
                        && m.ehdotusMetadata().tyyppi()
                            == MahdollisuudetController.MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS));
  }
}
