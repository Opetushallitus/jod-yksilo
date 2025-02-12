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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.config.koski.KoskiOAuth2Config;
import fi.okm.jod.yksilo.config.koski.TestKoskiOAuth2Config;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.koski.KoskiOAuth2Service;
import fi.okm.jod.yksilo.service.koski.KoskiService;
import fi.okm.jod.yksilo.util.TestUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@TestPropertySource(
    properties = "spring.security.oauth2.client.registration.koski.provider=koski-mtls")
@Import({ErrorInfoFactory.class, KoskiOAuth2Config.class, TestKoskiOAuth2Config.class})
@ActiveProfiles("test")
@WebMvcTest(IntegraatioKoskiController.class)
class IntegraatioKoskiControllerTest {

  private static final String EDUCATIONS_HISTORY_KOSKI_RESPONSE = "koski-response.json";
  private static final String API_KOSKI_KOULUTUKSET_ENDPOINT =
      "/api/integraatiot/koski/koulutukset";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private RestClient restClient;

  @MockitoBean private KoskiOAuth2Service koskiOAuth2Service;

  @MockitoBean private KoskiService koskiService;

  @WithMockUser
  @Test
  void shouldReturnEducationDataWhenAuthorized() throws Exception {
    var mockAuthorizedClient = mock(OAuth2AuthorizedClient.class);
    var mockDataInJson =
        objectMapper.readTree(
            TestUtil.getContentFromFile(EDUCATIONS_HISTORY_KOSKI_RESPONSE, KoskiService.class));

    when(koskiOAuth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(mockAuthorizedClient);
    when(koskiOAuth2Service.fetchDataFromResourceServer(mockAuthorizedClient))
        .thenReturn(mockDataInJson);
    when(koskiService.getKoulutusData(mockDataInJson, null)).thenCallRealMethod();

    mockMvc
        .perform(get(API_KOSKI_KOULUTUKSET_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
                    [{"id":null,"nimi":{},"kuvaus":{},"alkuPvm":"2006-01-01","loppuPvm":null,"osaamiset":null}]
                    """));

    verify(koskiOAuth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verify(koskiOAuth2Service).fetchDataFromResourceServer(mockAuthorizedClient);
    verify(koskiService).getKoulutusData(mockDataInJson, null);
    verifyNoMoreInteractions(koskiOAuth2Service, koskiService);
  }

  @WithMockUser
  @Test
  void shouldReturnUnauthorized_whenNotAuthorizedWithKoskiOAuth2() throws Exception {
    when(koskiOAuth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(null);

    mockMvc.perform(get(API_KOSKI_KOULUTUKSET_ENDPOINT)).andExpect(status().isUnauthorized());

    verify(koskiOAuth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verify(koskiOAuth2Service, never()).fetchDataFromResourceServer(any());
    verifyNoMoreInteractions(koskiOAuth2Service);
  }

  @WithMockUser
  @Test
  void shouldReturnUnauthorized_whenTokenExpired() throws Exception {
    var mockAuthorizedClient = mock(OAuth2AuthorizedClient.class);
    when(koskiOAuth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(mockAuthorizedClient);
    when(koskiOAuth2Service.fetchDataFromResourceServer(mockAuthorizedClient))
        .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Token is expired."));

    mockMvc
        .perform(get(API_KOSKI_KOULUTUKSET_ENDPOINT))
        .andDo(print())
        .andExpect(status().isUnauthorized());

    verify(koskiOAuth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verify(koskiOAuth2Service).fetchDataFromResourceServer(mockAuthorizedClient);
    verify(koskiService, never()).getKoulutusData(any(), any());
    verifyNoMoreInteractions(koskiOAuth2Service, koskiService);
  }

  @WithMockUser
  @Test
  void shouldReturnInternalServerError_whenFetchingDataFails() throws Exception {
    var mockAuthorizedClient = mock(OAuth2AuthorizedClient.class);
    when(koskiOAuth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(mockAuthorizedClient);
    when(koskiOAuth2Service.fetchDataFromResourceServer(mockAuthorizedClient))
        .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error."));

    mockMvc
        .perform(get(API_KOSKI_KOULUTUKSET_ENDPOINT))
        .andDo(print())
        .andExpect(status().isInternalServerError());

    verify(koskiOAuth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verify(koskiOAuth2Service).fetchDataFromResourceServer(mockAuthorizedClient);
    verify(koskiService, never()).getKoulutusData(any(), any());
    verifyNoMoreInteractions(koskiOAuth2Service, koskiService);
  }
}
