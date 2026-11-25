/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.koski;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.config.koski.KoskiOauth2Config;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.KoskiTehtavaTila;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.repository.KoulutusRepository;
import fi.okm.jod.yksilo.service.koski.KoskiService;
import fi.okm.jod.yksilo.service.koski.NoDataException;
import fi.okm.jod.yksilo.service.koski.ResourceServerException;
import fi.okm.jod.yksilo.service.koski.WrongPersonException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@TestPropertySource(properties = "jod.koski.enabled=true")
@Import({
  ErrorInfoFactory.class,
  KoskiOauth2Config.class,
  TestKoskiOauth2Config.class,
  KoskiService.class,
  MappingConfig.class
})
@WebMvcTest(IntegraatioKoskiController.class)
@Execution(ExecutionMode.SAME_THREAD)
class IntegraatioKoskiControllerTest {

  private static final String REGISTRATION_ID = "koski";
  private static final String API_KOSKI_KOULUTUKSET_ENDPOINT =
      "/api/integraatiot/koski/koulutukset";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private OAuth2AuthorizedClientRepository authorizedClients;

  @MockitoBean private KoskiService koskiService;

  @MockitoBean private KoulutusRepository koulutusRepository;

  @Autowired UserDetailsService userDetailsService;

  @WithUserDetails("test")
  @Test
  void shouldCreateKoskiTehtava_whenAuthorized() throws Exception {
    var token = prepareOauth2Client(Instant.now().plusSeconds(600));
    var tehtava = new KoskiTehtavaDto(UUID.randomUUID(), KoskiTehtavaTila.ODOTTAA, null);
    when(koskiService.startImport(any(JodUser.class), eq(token))).thenReturn(tehtava);

    mockMvc
        .perform(post(API_KOSKI_KOULUTUKSET_ENDPOINT).with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(tehtava.id().toString()))
        .andExpect(jsonPath("$.tila").value("ODOTTAA"));
  }

  @Test
  @WithUserDetails("test")
  void shouldReturnForbidden_whenNotAuthorizedWithKoskiOauth() throws Exception {
    var expectedResponseJson =
        """
            {"errorCode":"PERMISSION_REQUIRED","errorDetails":["Permission was not given or it is missing."]}
            """;
    performCreateKoskiTehtava(status().isForbidden(), expectedResponseJson);

    verify(authorizedClients)
        .loadAuthorizedClient(
            eq(REGISTRATION_ID), any(Authentication.class), any(HttpServletRequest.class));
    verifyNoMoreInteractions(authorizedClients);
    verifyNoInteractions(koskiService);
  }

  private OAuth2AccessToken prepareOauth2Client(Instant expiresAt) {
    var token =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, "token", expiresAt.minusSeconds(3600), expiresAt);
    var oauth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
    when(oauth2AuthorizedClient.getAccessToken()).thenReturn(token);
    when(authorizedClients.loadAuthorizedClient(
            eq(REGISTRATION_ID), any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(oauth2AuthorizedClient);
    return token;
  }

  private void performCreateKoskiTehtava(ResultMatcher expectedResult, String expectedResponseJson)
      throws Exception {
    mockMvc
        .perform(post(API_KOSKI_KOULUTUKSET_ENDPOINT).with(csrf()))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(expectedResult)
        .andExpect(content().json(expectedResponseJson));
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnInternalServerError_whenFetchingDataFails() throws Exception {
    var token = prepareOauth2Client(Instant.now().plusSeconds(600));
    when(koskiService.startImport(any(JodUser.class), eq(token)))
        .thenThrow(new ResourceServerException("Fail to get data from Koski resource server."));

    var expectedResponseJson =
        """
            {"errorCode":"SERVICE_ERROR","errorDetails":["Fail to get data from Koski resource server."]}
            """;
    performCreateKoskiTehtava(status().isInternalServerError(), expectedResponseJson);
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnWrongPersonError_whenPersonalIdDoesNotMatch() throws Exception {
    var token = prepareOauth2Client(Instant.now().plusSeconds(600));
    when(koskiService.startImport(any(JodUser.class), eq(token)))
        .thenThrow(new WrongPersonException(UUID.randomUUID()));

    var expectedResponseJson =
        """
            {"errorCode":"WRONG_PERSON","errorDetails":["Wrong person."]}
            """;
    performCreateKoskiTehtava(status().isForbidden(), expectedResponseJson);

    verify(authorizedClients)
        .removeAuthorizedClient(
            eq(REGISTRATION_ID),
            any(Authentication.class),
            any(HttpServletRequest.class),
            any(HttpServletResponse.class));
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnNoDataError_whenUserHaveNoDataInKoski() throws Exception {
    var token = prepareOauth2Client(Instant.now().plusSeconds(600));
    when(koskiService.startImport(any(JodUser.class), eq(token)))
        .thenThrow(
            new NoDataException(
                "omadataoauth2-error-94996a6c-a856-4dfd-8aee-da7edd578fe1: Oppijaa 1.2.246.562.24.51212001781 ei löydy tai käyttäjällä ei ole oikeuksia tietojen katseluun."));

    var expectedResponseJson =
        """
            {"errorCode":"DATA_NOT_FOUND","errorDetails":["The user either has no data or lacks access to retrieve it."]}
            """;
    performCreateKoskiTehtava(status().isForbidden(), expectedResponseJson);
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnForbidden_whenCreatingWithoutOauth() throws Exception {
    mockMvc
        .perform(post(API_KOSKI_KOULUTUKSET_ENDPOINT).with(csrf()))
        .andExpect(status().isForbidden());
    verifyNoInteractions(koskiService);
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnKoskiTehtavaStatus() throws Exception {
    var tehtavaId = UUID.randomUUID();
    var dto = new KoskiTehtavaDto(tehtavaId, KoskiTehtavaTila.VALMIS, null);
    when(koskiService.getStatus(any(JodUser.class), eq(tehtavaId))).thenReturn(dto);

    mockMvc
        .perform(get(API_KOSKI_KOULUTUKSET_ENDPOINT + "/" + tehtavaId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(tehtavaId.toString()));
  }

  @WithUserDetails("test")
  @Test
  void shouldSaveSelectedKoskiEducations() throws Exception {
    var tehtavaId = UUID.randomUUID();
    var body =
        """
            {
              "koulutuskokonaisuudet": [
                { "id": "%s", "lapset": ["%s"] }
              ]
            }
            """
            .formatted(UUID.randomUUID(), UUID.randomUUID());
    when(koskiService.save(any(JodUser.class), eq(tehtavaId), any()))
        .thenReturn(List.of(UUID.randomUUID()));

    mockMvc
        .perform(
            post(API_KOSKI_KOULUTUKSET_ENDPOINT + "/" + tehtavaId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNoContent());
  }

  @WithUserDetails("test")
  @Test
  void shouldDeleteKoskiTehtava() throws Exception {
    var tehtavaId = UUID.randomUUID();
    mockMvc
        .perform(delete(API_KOSKI_KOULUTUKSET_ENDPOINT + "/" + tehtavaId).with(csrf()))
        .andExpect(status().isNoContent());
    verify(koskiService).delete(any(JodUser.class), eq(tehtavaId));
  }
}
