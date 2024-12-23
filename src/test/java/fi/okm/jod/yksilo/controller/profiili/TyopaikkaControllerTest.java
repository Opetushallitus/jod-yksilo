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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.ToimenkuvaDto;
import fi.okm.jod.yksilo.dto.profiili.TyopaikkaDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.profiili.TyopaikkaService;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = TyopaikkaController.class)
@Import({ErrorInfoFactory.class, MappingConfig.class})
class TyopaikkaControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @MockitoBean private TyopaikkaService service;

  @Test
  @WithMockUser
  void shouldFailToAddInvalidTyopaikka() throws Exception {

    var dto =
        new TyopaikkaDto(
            null,
            new LocalizedString(Map.of(Kieli.FI, "testi")),
            Set.of(
                new ToimenkuvaDto(
                    null,
                    ls("Toimenkuva"),
                    null,
                    LocalDate.of(2024, 5, 1),
                    LocalDate.of(2023, 5, 1),
                    null)));
    mockMvc
        .perform(
            post("/api/profiili/tyopaikat")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().is4xxClientError());
  }
}
