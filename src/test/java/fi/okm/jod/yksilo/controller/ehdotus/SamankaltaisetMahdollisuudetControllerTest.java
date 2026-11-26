/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.ehdotus;

import static fi.okm.jod.yksilo.domain.MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS;
import static fi.okm.jod.yksilo.domain.MahdollisuusTyyppi.TYOMAHDOLLISUUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.config.SecurityConfig;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.TyomahdollisuusAineisto;
import fi.okm.jod.yksilo.domain.TyomahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.dto.ArvoDto;
import fi.okm.jod.yksilo.dto.JakaumaDto;
import fi.okm.jod.yksilo.dto.KoulutusmahdollisuusFullDto;
import fi.okm.jod.yksilo.dto.MahdollisuusDto;
import fi.okm.jod.yksilo.dto.OsaaminenDto;
import fi.okm.jod.yksilo.dto.tyomahdollisuus.TyomahdollisuusFullDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.AmmattiService;
import fi.okm.jod.yksilo.service.KoulutusmahdollisuusService;
import fi.okm.jod.yksilo.service.OsaaminenService;
import fi.okm.jod.yksilo.service.TyomahdollisuusService;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuudetService;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuusEhdotusService;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuusEhdotusService.EndpointProperties;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuusEhdotusService.Request;
import fi.okm.jod.yksilo.service.ehdotus.MahdollisuusEhdotusService.Response;
import fi.okm.jod.yksilo.service.ehdotus.Suggestion;
import fi.okm.jod.yksilo.service.inference.InferenceEndpoint;
import fi.okm.jod.yksilo.service.inference.InferenceService;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = SamankaltaisetMahdollisuudetController.class)
@Import({
  ErrorInfoFactory.class,
  MappingConfig.class,
  SecurityConfig.class,
  MahdollisuusEhdotusService.class
})
@Execution(ExecutionMode.SAME_THREAD)
class SamankaltaisetMahdollisuudetControllerTest {

  private static final URI OSAAMINEN = URI.create("urn:osaaminen1");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private InferenceService<Request, Response> inferenceService;
  @MockitoBean private MahdollisuudetService mahdollisuudetService;
  @MockitoBean private TyomahdollisuusService tyomahdollisuusService;
  @MockitoBean private KoulutusmahdollisuusService koulutusmahdollisuusService;
  @MockitoBean private OsaaminenService osaaminenService;
  @MockitoBean private AmmattiService ammattiService;

  @TestConfiguration
  static class TestConfig {
    @Bean
    EndpointProperties endpointProperties() {
      return new EndpointProperties(Map.of(Kieli.FI, "fi", Kieli.SV, "sv"));
    }
  }

  private final UUID lahdeId = UUID.randomUUID();
  private final List<UUID> tyoIds = ids(8);
  private final List<UUID> koulutusIds = ids(2);

  @BeforeEach
  void setUp() {
    var known = new LinkedHashMap<UUID, MahdollisuusDto>();
    known.put(lahdeId, mahdollisuus(lahdeId, TYOMAHDOLLISUUS));
    tyoIds.forEach(id -> known.put(id, mahdollisuus(id, TYOMAHDOLLISUUS)));
    koulutusIds.forEach(id -> known.put(id, mahdollisuus(id, KOULUTUSMAHDOLLISUUS)));
    when(mahdollisuudetService.fetchTyoAndKoulutusMahdollisuusIdsWithTypes(
            any(Sort.Direction.class), any(Kieli.class)))
        .thenReturn(known);
    when(osaaminenService.findBy(anySet()))
        .thenReturn(List.of(new OsaaminenDto(OSAAMINEN, null, null)));
    when(ammattiService.findBy(anySet())).thenReturn(List.of());
  }

  @Test
  void shouldReturnCacheableBestSuggestionsOfEachTypeForAnonymousUser() throws Exception {
    when(tyomahdollisuusService.get(lahdeId))
        .thenReturn(tyomahdollisuus(lahdeId, Map.of(Kieli.FI, "Kuvaus")));

    var response = new Response();
    // the source itself and a suggestion unknown to the backend are left out
    response.add(new Suggestion(lahdeId, 1.0, TYOMAHDOLLISUUS.name()));
    response.add(new Suggestion(UUID.randomUUID(), 0.99, TYOMAHDOLLISUUS.name()));
    // koulutusmahdollisuudet come after the työmahdollisuudet regardless of the score
    response.add(new Suggestion(koulutusIds.get(0), 0.95, KOULUTUSMAHDOLLISUUS.name()));
    // there is no score limit, as the scores are relative to the request
    response.add(new Suggestion(koulutusIds.get(1), 0.1, KOULUTUSMAHDOLLISUUS.name()));
    // only six of a type are returned
    IntStream.range(0, tyoIds.size())
        .forEach(
            i ->
                response.add(
                    new Suggestion(tyoIds.get(i), 0.9 - i * 0.01, TYOMAHDOLLISUUS.name())));
    when(inferenceService.infer(any(), any(), any())).thenReturn(response);

    var result =
        mockMvc
            .perform(get("/api/tyomahdollisuudet/{id}/samankaltaiset", lahdeId))
            .andExpect(status().isOk())
            .andExpect(
                header().string(HttpHeaders.CACHE_CONTROL, "max-age=3600, public, s-maxage=86400"))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
            .andExpect(jsonPath("$", hasSize(8)))
            .andExpect(
                jsonPath(
                    "$[*].id",
                    contains(
                        tyoIds.get(0).toString(),
                        tyoIds.get(1).toString(),
                        tyoIds.get(2).toString(),
                        tyoIds.get(3).toString(),
                        tyoIds.get(4).toString(),
                        tyoIds.get(5).toString(),
                        koulutusIds.get(0).toString(),
                        koulutusIds.get(1).toString())))
            .andExpect(jsonPath("$[6].tyyppi").value(KOULUTUSMAHDOLLISUUS.name()))
            .andReturn();

    assertThat(result.getRequest().getSession(false)).isNull();

    var request = ArgumentCaptor.forClass(Request.class);
    verify(inferenceService).infer(any(), request.capture(), any());
    assertThat(request.getValue().data().osaamiset()).containsExactly(OSAAMINEN);
    assertThat(request.getValue().data().osaamisetText()).isEqualTo("Kuvaus");
  }

  @Test
  void shouldSearchInFinnishAndPutTheSourceTypeFirst() throws Exception {
    when(koulutusmahdollisuusService.get(lahdeId))
        .thenReturn(
            koulutusmahdollisuus(lahdeId, Map.of(Kieli.FI, "Kuvaus", Kieli.SV, "Beskrivning")));

    var response = new Response();
    response.add(new Suggestion(tyoIds.get(0), 0.99, TYOMAHDOLLISUUS.name()));
    response.add(new Suggestion(koulutusIds.get(0), 0.6, KOULUTUSMAHDOLLISUUS.name()));
    when(inferenceService.infer(any(), any(), any())).thenReturn(response);

    mockMvc
        .perform(get("/api/koulutusmahdollisuudet/{id}/samankaltaiset", lahdeId))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[*].id", contains(koulutusIds.get(0).toString(), tyoIds.get(0).toString())));

    var request = ArgumentCaptor.forClass(Request.class);
    verify(inferenceService).infer(eq(InferenceEndpoint.of("fi")), request.capture(), any());
    assertThat(request.getValue().data().osaamisetText()).isEqualTo("Kuvaus");
    verify(mahdollisuudetService)
        .fetchTyoAndKoulutusMahdollisuusIdsWithTypes(Sort.Direction.ASC, Kieli.FI);
  }

  @Test
  void shouldFallBackToOtherLanguagesWithoutFinnishDescription() throws Exception {
    when(tyomahdollisuusService.get(lahdeId))
        .thenReturn(tyomahdollisuus(lahdeId, Map.of(Kieli.SV, "Beskrivning")));
    when(inferenceService.infer(any(), any(), any())).thenReturn(new Response());

    mockMvc
        .perform(get("/api/tyomahdollisuudet/{id}/samankaltaiset", lahdeId))
        .andExpect(status().isOk());

    var request = ArgumentCaptor.forClass(Request.class);
    verify(inferenceService).infer(any(), request.capture(), any());
    assertThat(request.getValue().data().osaamisetText()).isEqualTo("Beskrivning");
  }

  @Test
  void shouldNotCallInferenceWithoutInput() throws Exception {
    when(osaaminenService.findBy(anySet())).thenReturn(List.of());
    when(tyomahdollisuusService.get(lahdeId))
        .thenReturn(tyomahdollisuus(lahdeId, Map.of(Kieli.FI, " ")));

    mockMvc
        .perform(get("/api/tyomahdollisuudet/{id}/samankaltaiset", lahdeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));

    verify(inferenceService, never()).infer(any(), any(), any());
  }

  private static List<UUID> ids(int count) {
    return IntStream.range(0, count).mapToObj(i -> UUID.randomUUID()).toList();
  }

  private static MahdollisuusDto mahdollisuus(UUID id, MahdollisuusTyyppi tyyppi) {
    return new MahdollisuusDto(id, tyyppi, null, null, null, null, null, null, null, null, null);
  }

  private static JakaumaDto osaamisJakauma() {
    return new JakaumaDto(1, 0, List.of(new ArvoDto(OSAAMINEN.toString(), 1.0)));
  }

  private static TyomahdollisuusFullDto tyomahdollisuus(UUID id, Map<Kieli, String> kuvaus) {
    return new TyomahdollisuusFullDto(
        id,
        new LocalizedString(Map.of(Kieli.FI, "Otsikko")),
        null,
        new LocalizedString(kuvaus),
        null,
        null,
        null,
        TyomahdollisuusAineisto.TMT,
        true,
        Map.of(TyomahdollisuusJakaumaTyyppi.OSAAMINEN, osaamisJakauma()));
  }

  private static KoulutusmahdollisuusFullDto koulutusmahdollisuus(
      UUID id, Map<Kieli, String> kuvaus) {
    return new KoulutusmahdollisuusFullDto(
        id,
        KoulutusmahdollisuusTyyppi.TUTKINTO,
        new LocalizedString(Map.of(Kieli.FI, "Otsikko")),
        null,
        new LocalizedString(kuvaus),
        null,
        Set.of(),
        true,
        Map.of(KoulutusmahdollisuusJakaumaTyyppi.OSAAMINEN, osaamisJakauma()));
  }
}
