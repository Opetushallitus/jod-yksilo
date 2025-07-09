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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.IntegrationTest;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.OsaamisenTunnistusStatus;
import fi.okm.jod.yksilo.event.OsaamisetTunnistusEvent;
import fi.okm.jod.yksilo.event.OsaamisetTunnistusEventHandler;
import fi.okm.jod.yksilo.repository.KoulutusRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class KoulutusKokonaisuusControllerTestIT extends IntegrationTest {

  @MockitoBean private OsaamisetTunnistusEventHandler eventHandler;

  @Autowired private KoulutusRepository koulutusRepository;

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
                  {"id": null, "nimi": {"fi": "Koulutus 2", "sv": "Utbildning 2"}, "osasuoritukset": ["Kasvu, kehitys ja oppiminen (AVOIN YO)"]}
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

    var eventCaptor = ArgumentCaptor.forClass(OsaamisetTunnistusEvent.class);
    verify(eventHandler, timeout(5_000)).handleOsaamisetTunnistusEvent(eventCaptor.capture());
    var osaamisetTunnistusEvent = eventCaptor.getValue();
    var eventJodUser = osaamisetTunnistusEvent.jodUser();
    assertNotNull(eventJodUser);
    var eventKoulutukset = osaamisetTunnistusEvent.koulutukset();
    Assertions.assertEquals(2, eventKoulutukset.size());
    for (var koulutus : eventKoulutukset) {
      if (koulutus.getNimi().get(Kieli.FI).equals("Koulutus 1")) {
        assertNull(
            koulutus.getOsasuoritukset(),
            "Should not have any osasuoritukset, as they were not passed.");
      } else {
        assertNotNull(
            koulutus.getOsasuoritukset(),
            "Should have osasuoritukset, as they were passed. These are temporary in the event only.");
      }
    }

    var koulutukset =
        koulutusRepository.findAllById(eventKoulutukset.stream().map(Koulutus::getId).toList());
    for (var koulutus : koulutukset) {
      assertNull(koulutus.getOsasuoritukset(), "These should not been saved to DB.");
      assertEquals(OsaamisenTunnistusStatus.WAIT, koulutus.getOsaamisenTunnistusStatus());
    }
  }
}
