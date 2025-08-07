/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.IntegrationTest;
import fi.okm.jod.yksilo.domain.SuosikkiTyyppi;
import fi.okm.jod.yksilo.dto.profiili.SuosikkiDto;
import fi.okm.jod.yksilo.entity.YksilonSuosikki;
import fi.okm.jod.yksilo.repository.YksilonSuosikkiRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/** Integrationtests for {@link YksilonSuosikkiController} */
@AutoConfigureMockMvc
class YksilonSuosikkiControllerIT extends IntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private YksilonSuosikkiRepository suosikkiRepository;

  @WithUserDetails("test")
  @Sql(
      scripts = {"/data/add-koulutusmahdollisuus.sql"},
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(
      scripts = {"/data/cleanup.sql"},
      executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  @Transactional
  @Test
  void yksilonSuosikkiAdd200() throws Exception {
    Instant afterCreationOfYksilo = Instant.now();
    UUID koulutusMahdollisuusId = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
    SuosikkiDto newSuosikki =
        new SuosikkiDto(null, koulutusMahdollisuusId, SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS, null);
    final MvcResult mvcResult =
        this.mockMvc
            .perform(
                post("/api/profiili/suosikit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(this.objectMapper.writeValueAsString(newSuosikki)))
            .andExpect(status().isOk())
            .andReturn();

    // lisää koulutusmahdollisuus
    final UUID addedId = getResponse(mvcResult, UUID.class);
    final YksilonSuosikki addedSuosikki =
        suosikkiRepository.findById(addedId).orElseThrow(RuntimeException::new);
    final Instant yksiloMuokattu = addedSuosikki.getYksilo().getMuokattu();
    assertEquals(
        koulutusMahdollisuusId,
        addedSuosikki.getKoulutusmahdollisuus().getId(),
        "Suosikilla on sama koulutusmahdollisuus kuin dto:ssa");
    assertEquals(
        yksiloMuokattu.isAfter(afterCreationOfYksilo),
        "Yksilön aikaleimaa tulee päivittää kun suosikki luodaan");
  }
}
