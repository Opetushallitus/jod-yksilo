/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.ehdotus;

import static fi.okm.jod.yksilo.domain.MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS;
import static fi.okm.jod.yksilo.domain.MahdollisuusTyyppi.TYOMAHDOLLISUUS;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.controller.ehdotus.MahdollisuudetController.EndpointProperties;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.dto.PolunVaiheEhdotusDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.AmmattiService;
import fi.okm.jod.yksilo.service.OsaaminenService;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuudetService;
import fi.okm.jod.yksilo.service.inference.InferenceService;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(value = MahdollisuudetController.class)
@Import({
  ErrorInfoFactory.class,
  MappingConfig.class,
})
@Execution(ExecutionMode.SAME_THREAD)
class MahdollisuudetControllerTest {

  @Autowired ObjectMapper objectMapper;
  @Autowired private MockMvc mockMvc;
  @MockitoBean private MahdollisuudetService mahdollisuudetService;

  @MockitoBean private OsaaminenService osaaminenService;
  @MockitoBean private AmmattiService ammattiService;

  @MockitoBean
  private InferenceService<MahdollisuudetController.Request, MahdollisuudetController.Response>
      inferenceService;

  @TestConfiguration
  static class TestConfig {
    @Bean
    EndpointProperties endpointProperties() {
      return new EndpointProperties(Map.of(Kieli.FI, "fi"));
    }
  }

  @Test
  @WithMockUser
  void whenEmptySkillsShouldReturnEmptySet() throws Exception {
    var luoEhdotusDto =
        new MahdollisuudetController.LuoEhdotusDto(
            Collections.emptySet(), null, 0.5, Collections.emptySet(), null, 0.5, 0.5, 0.5);

    var listOfIds = IntStream.range(0, 6).mapToObj(i -> UUID.randomUUID()).toList();

    LinkedHashMap<UUID, MahdollisuusTyyppi> mahdollisuudet = LinkedHashMap.newLinkedHashMap(3);
    mahdollisuudet.putAll(
        Map.of(
            listOfIds.get(0), TYOMAHDOLLISUUS,
            listOfIds.get(1), KOULUTUSMAHDOLLISUUS,
            listOfIds.get(2), TYOMAHDOLLISUUS,
            listOfIds.get(3), TYOMAHDOLLISUUS,
            listOfIds.get(4), KOULUTUSMAHDOLLISUUS,
            listOfIds.get(5), TYOMAHDOLLISUUS));

    when(mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
            Sort.Direction.ASC, Kieli.FI))
        .thenReturn(mahdollisuudet);
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
            .filter(m -> m.ehdotusMetadata().tyyppi() == TYOMAHDOLLISUUS)
            .allMatch(
                m ->
                    mahdollisuudet.containsKey(m.mahdollisuusId())
                        && m.ehdotusMetadata().pisteet() == null
                        && m.ehdotusMetadata().trendi() == null
                        && m.ehdotusMetadata().tyollisyysNakyma() == null));

    // test all koulutusmahdollisuudet are included in response with empty metadata with type
    // KOULUTUSMAHDOLLISUUS
    assertTrue(
        r.stream()
            .filter(m -> m.ehdotusMetadata().tyyppi() == KOULUTUSMAHDOLLISUUS)
            .allMatch(
                m ->
                    mahdollisuudet.containsKey(m.mahdollisuusId())
                        && m.ehdotusMetadata().pisteet() == null
                        && m.ehdotusMetadata().trendi() == null
                        && m.ehdotusMetadata().tyollisyysNakyma() == null));
  }

  @Test
  @WithMockUser
  void whenSkillsShouldReturnScores() throws Exception {
    final var luoEhdotusDto =
        new MahdollisuudetController.LuoEhdotusDto(
            Set.of(URI.create("urn:osaaminen1")),
            null,
            0.5,
            Set.of(URI.create("urn:osaaminen2")),
            null,
            0.5,
            0.5,
            0.5);

    var listOfIds = IntStream.range(0, 6).mapToObj(i -> UUID.randomUUID()).toList();

    LinkedHashMap<UUID, MahdollisuusTyyppi> mahdollisuudet = LinkedHashMap.newLinkedHashMap(3);
    mahdollisuudet.putAll(
        Map.of(
            listOfIds.get(0), TYOMAHDOLLISUUS,
            listOfIds.get(1), KOULUTUSMAHDOLLISUUS,
            listOfIds.get(2), TYOMAHDOLLISUUS,
            listOfIds.get(3), TYOMAHDOLLISUUS,
            listOfIds.get(4), KOULUTUSMAHDOLLISUUS,
            listOfIds.get(5), TYOMAHDOLLISUUS));
    var inferenceResponse = new MahdollisuudetController.Response();
    inferenceResponse.addAll(
        List.of(
            new Suggestion(listOfIds.get(0), 0.99d, TYOMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(1), 0.89d, KOULUTUSMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(2), 0.79d, TYOMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(3), 0.98d, TYOMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(4), 0.88d, KOULUTUSMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(5), 0.78d, TYOMAHDOLLISUUS.name())));

    var osaamiset =
        List.of(
            new OsaaminenDto(
                URI.create("urn:osaaminen1"),
                new LocalizedString(Map.of(Kieli.FI, "text")),
                new LocalizedString(Map.of(Kieli.FI, "text"))));

    when(mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
            Sort.Direction.ASC, Kieli.FI))
        .thenReturn(mahdollisuudet);
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
            .filter(m -> m.ehdotusMetadata().tyyppi() == TYOMAHDOLLISUUS)
            .allMatch(
                m ->
                    mahdollisuudet.containsKey(m.mahdollisuusId())
                        && m.ehdotusMetadata().pisteet() != null
                        && m.ehdotusMetadata().pisteet() > 0
                        && m.ehdotusMetadata().trendi() == null
                        && m.ehdotusMetadata().tyollisyysNakyma() == null));

    // test all koulutusmahdollisuudet are included in response with empty metadata with type
    // KOULUTUSMAHDOLLISUUS
    assertTrue(
        r.stream()
            .filter(m -> m.ehdotusMetadata().tyyppi() == KOULUTUSMAHDOLLISUUS)
            .allMatch(
                m ->
                    mahdollisuudet.containsKey(m.mahdollisuusId())
                        && m.ehdotusMetadata().pisteet() != null
                        && m.ehdotusMetadata().pisteet() > 0
                        && m.ehdotusMetadata().trendi() == null
                        && m.ehdotusMetadata().tyollisyysNakyma() == null));
  }

  @Test
  @WithMockUser
  void ifInferenceReturnsUnknownIdShouldNotCauseNpe() throws Exception {
    final var luoEhdotusDto =
        new MahdollisuudetController.LuoEhdotusDto(
            Set.of(URI.create("http://dymmy")),
            null,
            0.5,
            Set.of(URI.create("http://dymmy")),
            null,
            0.5,
            0.5,
            0.5);

    var listOfIds = IntStream.range(0, 6).mapToObj(i -> UUID.randomUUID()).toList();

    LinkedHashMap<UUID, MahdollisuusTyyppi> mahdollisuudet = LinkedHashMap.newLinkedHashMap(3);
    mahdollisuudet.putAll(
        Map.of(
            listOfIds.get(0), TYOMAHDOLLISUUS,
            listOfIds.get(1), KOULUTUSMAHDOLLISUUS,
            listOfIds.get(2), TYOMAHDOLLISUUS,
            listOfIds.get(3), TYOMAHDOLLISUUS,
            listOfIds.get(4), KOULUTUSMAHDOLLISUUS,
            listOfIds.get(5), TYOMAHDOLLISUUS));
    var inferenceResponse = new MahdollisuudetController.Response();
    inferenceResponse.addAll(
        List.of(
            new Suggestion(listOfIds.get(0), 0.99d, TYOMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(1), 0.89d, KOULUTUSMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(2), 0.79d, TYOMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(3), 0.98d, TYOMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(4), 0.88d, KOULUTUSMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(5), 0.78d, TYOMAHDOLLISUUS.name())));

    var osaamiset =
        List.of(
            new OsaaminenDto(
                URI.create("urn:osaaminen1"),
                new LocalizedString(Map.of(Kieli.FI, "text")),
                new LocalizedString(Map.of(Kieli.FI, "text"))));

    when(mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
            Sort.Direction.ASC, Kieli.FI))
        .thenReturn(mahdollisuudet);
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
  void ifInferenceReturnsMissingIdShouldNotCauseNpe() throws Exception {
    final var luoEhdotusDto =
        new MahdollisuudetController.LuoEhdotusDto(
            Set.of(URI.create("http://dymmy")),
            null,
            0.5,
            Set.of(URI.create("http://dymmy")),
            null,
            0.5,
            0.5,
            0.5);

    var listOfIds = IntStream.range(0, 6).mapToObj(i -> UUID.randomUUID()).toList();

    LinkedHashMap<UUID, MahdollisuusTyyppi> mahdollisuudet = LinkedHashMap.newLinkedHashMap(3);
    mahdollisuudet.putAll(
        Map.of(
            listOfIds.get(0), TYOMAHDOLLISUUS,
            listOfIds.get(1), KOULUTUSMAHDOLLISUUS,
            listOfIds.get(2), TYOMAHDOLLISUUS,
            listOfIds.get(3), TYOMAHDOLLISUUS,
            listOfIds.get(4), KOULUTUSMAHDOLLISUUS,
            listOfIds.get(5), TYOMAHDOLLISUUS));
    var inferenceResponse = new MahdollisuudetController.Response();
    inferenceResponse.addAll(
        List.of(
            new Suggestion(listOfIds.get(0), 0.99d, TYOMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(1), 0.89d, KOULUTUSMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(2), 0.79d, TYOMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(3), 0.98d, TYOMAHDOLLISUUS.name()),
            new Suggestion(listOfIds.get(4), 0.88d, KOULUTUSMAHDOLLISUUS.name()),
            new Suggestion(UUID.randomUUID(), 0.78d, TYOMAHDOLLISUUS.name())));
    var osaamiset =
        List.of(
            new OsaaminenDto(
                URI.create("urn:osaaminen1"),
                new LocalizedString(Map.of(Kieli.FI, "text")),
                new LocalizedString(Map.of(Kieli.FI, "text"))));

    when(mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
            Sort.Direction.ASC, Kieli.FI))
        .thenReturn(mahdollisuudet);
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
    // assert that missing tyomahdollisusu was returned as empty with proper type
    assertTrue(
        r.stream()
            .filter(m -> m.mahdollisuusId() == listOfIds.get(5))
            .allMatch(
                m ->
                    m.ehdotusMetadata().pisteet() == null
                        && m.ehdotusMetadata().tyyppi() == TYOMAHDOLLISUUS));
  }

  @Test
  @WithMockUser
  void shouldGetMahdollisuudetSuggestionsForPolkuVaihe() throws Exception {
    var missingOsaamiset = Set.of(URI.create("urn:osaaminen1"), URI.create("urn:osaaminen2"));

    var serviceSuggestions =
        List.of(
            new PolunVaiheEhdotusDto(
                UUID.fromString("481e204a-691a-48dd-9b01-7f08d5858db9"), 0.5, 2));
    when(mahdollisuudetService.getPolkuVaiheSuggestions(missingOsaamiset))
        .thenReturn(serviceSuggestions);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/ehdotus/mahdollisuudet/polku")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingOsaamiset)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(
            jsonPath("$[0].mahdollisuusId")
                .value(serviceSuggestions.get(0).mahdollisuusId().toString()));
  }

  @Test
  @WithMockUser
  void shouldReturnEmptyResponseWhenNoSuggestionsFound() throws Exception {
    var missingOsaamiset = Set.of(URI.create("urn:nonexistent"));
    when(mahdollisuudetService.getPolkuVaiheSuggestions(missingOsaamiset))
        .thenReturn(Collections.emptyList());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/ehdotus/mahdollisuudet/polku")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(missingOsaamiset)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    verify(mahdollisuudetService).getPolkuVaiheSuggestions(missingOsaamiset);
  }

  @Test
  @WithMockUser
  void shouldHandleEmptyMissingOsaamiset() throws Exception {
    Set<URI> emptyOsaamiset = Collections.emptySet();
    when(mahdollisuudetService.getPolkuVaiheSuggestions(emptyOsaamiset))
        .thenReturn(Collections.emptyList());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/ehdotus/mahdollisuudet/polku")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyOsaamiset)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }
}
