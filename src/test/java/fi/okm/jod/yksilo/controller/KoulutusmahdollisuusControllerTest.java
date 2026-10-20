/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import static fi.okm.jod.yksilo.domain.MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS;
import static fi.okm.jod.yksilo.domain.MahdollisuusTyyppi.TYOMAHDOLLISUUS;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.SamankaltainenMahdollisuusDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.KoulutusmahdollisuusService;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.SamankaltaisetMahdollisuudetService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(value = KoulutusmahdollisuusController.class)
@Import({ErrorInfoFactory.class, MappingConfig.class})
@Execution(ExecutionMode.SAME_THREAD)
class KoulutusmahdollisuusControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private KoulutusmahdollisuusService service;
  @MockitoBean private SamankaltaisetMahdollisuudetService samankaltaisetService;

  @Test
  @WithMockUser
  void shouldFindSamankaltaiset() throws Exception {
    var id = UUID.randomUUID();
    var otherId = UUID.randomUUID();
    when(samankaltaisetService.find(KOULUTUSMAHDOLLISUUS, id, KOULUTUSMAHDOLLISUUS))
        .thenReturn(
            List.of(
                new SamankaltainenMahdollisuusDto(
                    otherId, new LocalizedString(Map.of(Kieli.FI, "Toinen")))));

    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/koulutusmahdollisuudet/{id}/samankaltaiset", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id", is(otherId.toString())))
        .andExpect(jsonPath("$[0].otsikko.fi", is("Toinen")));
  }

  @Test
  @WithMockUser
  void shouldReturnNotFoundForUnknownSamankaltaiset() throws Exception {
    var id = UUID.randomUUID();
    when(samankaltaisetService.find(KOULUTUSMAHDOLLISUUS, id, KOULUTUSMAHDOLLISUUS))
        .thenThrow(new NotFoundException("Unknown Koulutusmahdollisuus"));

    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/koulutusmahdollisuudet/{id}/samankaltaiset", id))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser
  void shouldFindSamankaltaisetOfRequestedType() throws Exception {
    var id = UUID.randomUUID();
    var otherId = UUID.randomUUID();
    when(samankaltaisetService.find(KOULUTUSMAHDOLLISUUS, id, TYOMAHDOLLISUUS))
        .thenReturn(
            List.of(
                new SamankaltainenMahdollisuusDto(
                    otherId, new LocalizedString(Map.of(Kieli.FI, "Työ")))));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/koulutusmahdollisuudet/{id}/samankaltaiset", id)
                .param("tyyppi", "TYOMAHDOLLISUUS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id", is(otherId.toString())));
  }
}
