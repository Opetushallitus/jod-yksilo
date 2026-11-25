/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class KoulutusKokonaisuusControllerTest extends IntegrationTest {

  @Autowired private MockMvc mockMvc;

  @WithUserDetails("test")
  @Test
  void addMany() throws Exception {
    var payload =
        """
            [
              {
                "id": null,
                "nimi": {"fi": "Testi nimi", "sv": "Test namn"},
                "koulutukset": [
                  {"id": null, "nimi": {"fi": "Koulutus 1", "sv": "Utbildning 1"}},
                  {"id": null, "nimi": {"fi": "Koulutus 2", "sv": "Utbildning 2"}}
                ]
              }
            ]
            """;

    mockMvc
        .perform(
            post("/api/profiili/koulutuskokonaisuudet/tuonti")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));
  }
}
