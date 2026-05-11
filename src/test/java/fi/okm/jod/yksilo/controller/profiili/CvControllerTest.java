/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.config.CvProperties;
import fi.okm.jod.yksilo.config.mocklogin.MockJodUserImpl;
import fi.okm.jod.yksilo.domain.CvTehtavaTila;
import fi.okm.jod.yksilo.dto.profiili.CvTehtavaDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceConflictException;
import fi.okm.jod.yksilo.service.profiili.cv.CvService;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = CvController.class)
@Import({ErrorInfoFactory.class})
@EnableWebSecurity
@Execution(ExecutionMode.SAME_THREAD)
class CvControllerTest {

  private static final int MAX_SIZE = 1000;

  @MockitoBean private CvService cvService;
  @Autowired private MockMvc mockMvc;

  @TestConfiguration
  static class TestConfig {

    @Bean
    UserDetailsService mockUserDetailsService() {
      return username -> new MockJodUserImpl(username, UUID.randomUUID());
    }

    @Bean
    CvProperties cvProperties() {
      return new CvProperties(null, null, MAX_SIZE, null, null);
    }
  }

  private static final byte[] VALID_PDF;
  private static final byte[] INVALID_BYTES;
  private static final byte[] EMPTY_BODY = new byte[0];
  private static final byte[] OVERSIZED = new byte[MAX_SIZE + 1];

  static {
    try (var pdf = CvController.class.getResourceAsStream("/data/test.pdf");
        var invalid = CvController.class.getResourceAsStream("/data/invalid.pdf")) {
      VALID_PDF = pdf.readAllBytes();
      INVALID_BYTES = invalid.readAllBytes();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load test PDF", e);
    }
    Arrays.fill(OVERSIZED, (byte) '\n');
    System.arraycopy("%PDF-1.0".getBytes(), 0, OVERSIZED, 0, 8);
    System.arraycopy("%%EOF".getBytes(), 0, OVERSIZED, OVERSIZED.length - 5, 5);
  }

  @Test
  @WithUserDetails("test")
  void shouldAcceptValidPdf() throws Exception {
    var taskId = UUID.randomUUID();
    when(cvService.submit(any(), any(), any()))
        .thenReturn(new CvTehtavaDto(taskId, CvTehtavaTila.ODOTTAA, null));

    mockMvc
        .perform(
            post("/api/profiili/cv").with(csrf()).contentType("application/pdf").content(VALID_PDF))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.id").value(taskId.toString()))
        .andExpect(jsonPath("$.tila").value("ODOTTAA"))
        .andExpect(jsonPath("$.tulos").isEmpty());
  }

  @Test
  @WithUserDetails("test")
  void shouldRejectIfInFlightTaskExists() throws Exception {
    doThrow(new ServiceConflictException("In-flight task exists"))
        .when(cvService)
        .checkNoInFlightTask(any());

    mockMvc
        .perform(
            post("/api/profiili/cv").with(csrf()).contentType("application/pdf").content(VALID_PDF))
        .andExpect(status().isConflict());
  }

  @Test
  @WithUserDetails("test")
  void shouldRejectNonPdfContent() throws Exception {
    mockMvc
        .perform(
            post("/api/profiili/cv")
                .with(csrf())
                .contentType("application/pdf")
                .content(INVALID_BYTES))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("test")
  void shouldRejectEmptyBody() throws Exception {
    mockMvc
        .perform(
            post("/api/profiili/cv")
                .with(csrf())
                .contentType("application/pdf")
                .content(EMPTY_BODY))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("test")
  void shouldRejectOversizedBody() throws Exception {
    mockMvc
        .perform(
            post("/api/profiili/cv").with(csrf()).contentType("application/pdf").content(OVERSIZED))
        .andExpect(status().isContentTooLarge());
  }

  @Test
  void shouldRejectUnauthenticated() throws Exception {
    mockMvc
        .perform(
            post("/api/profiili/cv").with(csrf()).contentType("application/pdf").content(VALID_PDF))
        .andExpect(status().is4xxClientError());
  }

  @Test
  @WithUserDetails("test")
  void shouldReturnTaskStatus() throws Exception {
    var taskId = UUID.randomUUID();
    when(cvService.getStatus(any(), any()))
        .thenReturn(new CvTehtavaDto(taskId, CvTehtavaTila.ODOTTAA, null));

    mockMvc
        .perform(get("/api/profiili/cv/{tehtavaId}", taskId).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(taskId.toString()))
        .andExpect(jsonPath("$.tila").value("ODOTTAA"));
  }

  @Test
  @WithUserDetails("test")
  void shouldReturn404ForUnknownTask() throws Exception {
    when(cvService.getStatus(any(), any())).thenThrow(new NotFoundException("Not found"));

    mockMvc
        .perform(get("/api/profiili/cv/{tehtavaId}", UUID.randomUUID()).with(csrf()))
        .andExpect(status().isNotFound());
  }
}
