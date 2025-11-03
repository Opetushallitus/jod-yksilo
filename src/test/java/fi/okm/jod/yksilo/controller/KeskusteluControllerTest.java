/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.config.SecurityConfig;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.controller.KeskusteluController.InferenceRequest;
import fi.okm.jod.yksilo.controller.KeskusteluController.InferenceResponse;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.inference.InferenceService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = KeskusteluController.class)
@Import({ErrorInfoFactory.class, MappingConfig.class, SecurityConfig.class})
class KeskusteluControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper mapper;

  @TestConfiguration
  static class TestConfig {
    @Bean
    public InferenceService<InferenceRequest, InferenceResponse> inferenceService() {
      return (endpoint, payload, responseType) -> {
        if (payload.session() == null) {
          var sessionId = UUID.randomUUID();
          var session =
              new KeskusteluController.InferenceSession(
                  sessionId, Instant.now().getEpochSecond(), "dummy-signature");
          return new InferenceResponse(session, Set.of(), payload.message());
        }
        return new InferenceResponse(payload.session(), Set.of(), payload.message());
      };
    }
  }

  @Test
  void shouldStartNewConversation() throws Exception {
    mockMvc
        .perform(
            post("/api/keskustelut")
                .contentType("application/json")
                .content(mapper.writeValueAsString(ls(Kieli.FI, "START"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vastaus").value("START"));
  }

  @Test
  void shouldContinueConversation() throws Exception {
    var session = new MockHttpSession();
    var result =
        mockMvc
            .perform(
                post("/api/keskustelut")
                    .session(session)
                    .contentType("application/json")
                    .content(mapper.writeValueAsString(ls(Kieli.FI, "START"))))
            .andExpect(status().isOk())
            .andReturn();

    var response = mapper.readValue(result.getResponse().getContentAsString(), JsonNode.class);
    mockMvc
        .perform(
            post("/api/keskustelut/{id}", response.path("id").asText())
                .session(session)
                .contentType("application/json")
                .content(mapper.writeValueAsString(ls(Kieli.FI, "CONTINUE"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.vastaus").value("CONTINUE"));
  }

  @Test
  void shouldNotContinueNonExistingConversation() throws Exception {
    var session = new MockHttpSession();
    mockMvc
        .perform(
            post("/api/keskustelut")
                .session(session)
                .contentType("application/json")
                .content(mapper.writeValueAsString(ls(Kieli.FI, "START"))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/keskustelut/{id}", UUID.randomUUID())
                .session(session)
                .contentType("application/json")
                .content(mapper.writeValueAsString(ls(Kieli.FI, "CONTINUE"))))
        .andExpect(status().isNotFound());
  }
}
