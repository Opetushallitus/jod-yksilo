/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.koski;

import static fi.okm.jod.yksilo.testutil.TestUtil.authenticateUser;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.config.koski.KoskiOAuth2Config;
import fi.okm.jod.yksilo.config.koski.TestKoskiOAuth2Config;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.koski.KoskiOAuth2Service;
import fi.okm.jod.yksilo.service.koski.KoskiService;
import fi.okm.jod.yksilo.service.koski.PermissionRequiredException;
import fi.okm.jod.yksilo.service.koski.ResourceServerException;
import fi.okm.jod.yksilo.testutil.TestUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;

@TestPropertySource(properties = "jod.koski.enabled=true")
@Import({ErrorInfoFactory.class, KoskiOAuth2Config.class, TestKoskiOAuth2Config.class})
@WebMvcTest(IntegraatioKoskiController.class)
class IntegraatioKoskiControllerTest {

  private static final String EDUCATIONS_HISTORY_KOSKI_RESPONSE = "koski-response.json";
  private static final String API_KOSKI_KOULUTUKSET_ENDPOINT =
      "/api/integraatiot/koski/koulutukset";

  private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private RestClient restClient;

  @MockitoBean private KoskiOAuth2Service koskiOAuth2Service;

  @MockitoBean private KoskiService koskiService;

  @Autowired private WebApplicationContext webApplicationContext;

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
  }

  @WithMockUser
  @Test
  void shouldReturnEducationDataWhenAuthorized() throws Exception {
    var jodUser = mockJodUser();
    authenticateUser(jodUser);

    var mockAuthorizedClient = prepareOAuth2Client();
    var mockDataInJson =
        objectMapper.readTree(
            TestUtil.getContentFromFile(EDUCATIONS_HISTORY_KOSKI_RESPONSE, KoskiService.class));
    when(koskiOAuth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(mockAuthorizedClient);
    when(koskiOAuth2Service.fetchDataFromResourceServer(mockAuthorizedClient))
        .thenReturn(mockDataInJson);
    when(koskiService.getKoulutusData(mockDataInJson, null))
        .thenReturn(
            List.of(
                new KoulutusDto(
                    null, // id
                    new LocalizedString(
                        new HashMap<>() {
                          {
                            put(Kieli.FI, "Nimi");
                            put(Kieli.EN, "Name");
                            put(Kieli.SV, "Namm");
                          }
                        }),
                    new LocalizedString(
                        new HashMap<>() {
                          {
                            put(Kieli.FI, "Kuvaus");
                            put(Kieli.EN, "Description");
                            put(Kieli.SV, "Beskrivning");
                          }
                        }),
                    LocalDate.of(2006, 1, 1), // alkuPvm
                    null, // loppuPvm is null
                    null // osaamiset is null
                    )));
    doAnswer(invocationOnMock -> null)
        .when(koskiOAuth2Service)
        .checkPersonIdMatches(any(JodUser.class), any(JsonNode.class));

    var expectedResponseJson =
        """
        [{"id":null,"nimi":{},"kuvaus":{},"alkuPvm":"2006-01-01","loppuPvm":null,"osaamiset":null}]
        """;
    performGetEducationsDataFromKoski(mockAuthorizedClient, status().isOk(), expectedResponseJson);

    verify(koskiOAuth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verify(koskiOAuth2Service).fetchDataFromResourceServer(mockAuthorizedClient);
    verify(koskiOAuth2Service).checkPersonIdMatches(any(), any(JsonNode.class));
    verify(koskiService).getKoulutusData(mockDataInJson, null);
    verifyNoMoreInteractions(koskiOAuth2Service, koskiService);
  }

  @WithMockUser
  @Test
  void shouldReturnUnauthorized_whenNotAuthorizedWithKoskiOAuth() throws Exception {
    var jodUser = mockJodUser();
    authenticateUser(jodUser);

    var oAuth2AuthorizedClient = prepareOAuth2Client();
    when(koskiOAuth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(null);

    var expectedResponseJson =
        """
        {"errorCode":"PERMISSION_REQUIRED","errorDetails":["Permission was not given or it is missing."]}
        """;
    performGetEducationsDataFromKoski(
        oAuth2AuthorizedClient, status().isForbidden(), expectedResponseJson);

    verify(koskiOAuth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verifyNoMoreInteractions(koskiOAuth2Service);
    verifyNoInteractions(koskiService);
  }

  private static JodUser mockJodUser() {
    var jodUser = mock(JodUser.class);
    when(jodUser.getId()).thenReturn(UUID.fromString("6d910473-ced6-46a6-93a9-d5090008ae1c"));
    return jodUser;
  }

  private OAuth2AuthorizedClient prepareOAuth2Client() {
    var clientRegistration = mock(ClientRegistration.class);
    when(clientRegistration.getRegistrationId()).thenReturn("koski");

    var oAuth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
    when(oAuth2AuthorizedClient.getClientRegistration()).thenReturn(clientRegistration);
    when(koskiOAuth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(oAuth2AuthorizedClient);

    return oAuth2AuthorizedClient;
  }

  @WithMockUser
  @Test
  void shouldReturnUnauthorized_whenTokenExpired() throws Exception {
    var jodUser = mockJodUser();
    authenticateUser(jodUser);

    var oAuth2AuthorizedClient = prepareOAuth2Client();
    when(koskiOAuth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(oAuth2AuthorizedClient);
    when(koskiOAuth2Service.fetchDataFromResourceServer(oAuth2AuthorizedClient))
        .thenThrow(new PermissionRequiredException("Token expired."));

    var expectedResponseJson =
        """
        {"errorCode":"PERMISSION_REQUIRED","errorDetails":["Token expired."]}
        """;
    performGetEducationsDataFromKoski(
        oAuth2AuthorizedClient, status().isForbidden(), expectedResponseJson);

    verify(koskiOAuth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verify(koskiOAuth2Service).fetchDataFromResourceServer(oAuth2AuthorizedClient);
    verifyNoMoreInteractions(koskiOAuth2Service);
    verifyNoInteractions(koskiService);
  }

  private void performGetEducationsDataFromKoski(
      OAuth2AuthorizedClient oAuth2AuthorizedClient,
      ResultMatcher expectedResult,
      String expectedResponseJson)
      throws Exception {
    mockMvc
        .perform(
            get(API_KOSKI_KOULUTUKSET_ENDPOINT)
                .principal(new TestingAuthenticationToken(oAuth2AuthorizedClient, null)))
        .andDo(print())
        .andExpect(expectedResult)
        .andExpect(content().json(expectedResponseJson));
  }

  @WithMockUser
  @Test
  void shouldReturnInternalServerError_whenFetchingDataFails() throws Exception {
    var jodUser = mockJodUser();
    authenticateUser(jodUser);

    var oAuth2AuthorizedClient = prepareOAuth2Client();
    when(koskiOAuth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(oAuth2AuthorizedClient);
    var exception = mock(HttpClientErrorException.class);
    when(exception.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
    when(koskiOAuth2Service.fetchDataFromResourceServer(oAuth2AuthorizedClient))
        .thenThrow(
            new ResourceServerException("Fail to get data from Koski resource server.", exception));

    var expectedResponseJson =
        """
        {"errorCode":"SERVICE_ERROR","errorDetails":["Fail to get data from Koski resource server."]}
        """;
    performGetEducationsDataFromKoski(
        oAuth2AuthorizedClient, status().isInternalServerError(), expectedResponseJson);

    verify(koskiOAuth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verify(koskiOAuth2Service).fetchDataFromResourceServer(oAuth2AuthorizedClient);
    verify(koskiService, never()).getKoulutusData(any(), any());
    verifyNoMoreInteractions(koskiOAuth2Service, koskiService);
  }
}
