/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.koski;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.config.koski.KoskiOAuth2Config;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.koski.KoskiOAuth2Service;
import fi.okm.jod.yksilo.service.koski.KoskiService;
import fi.okm.jod.yksilo.service.koski.NoDataException;
import fi.okm.jod.yksilo.service.koski.PermissionRequiredException;
import fi.okm.jod.yksilo.service.koski.ResourceServerException;
import fi.okm.jod.yksilo.service.koski.WrongPersonException;
import fi.okm.jod.yksilo.testutil.TestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@TestPropertySource(properties = "jod.koski.enabled=true")
@Import({ErrorInfoFactory.class, KoskiOAuth2Config.class, TestKoskiOAuth2Config.class})
@WebMvcTest(IntegraatioKoskiController.class)
class IntegraatioKoskiControllerTest {

  private static final String EDUCATIONS_HISTORY_KOSKI_RESPONSE = "koski-response.json";
  private static final String API_KOSKI_KOULUTUKSET_ENDPOINT =
      "/api/integraatiot/koski/koulutukset";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private KoskiOAuth2Service koskiOAuth2Service;

  @MockitoBean private KoskiService koskiService;

  @WithUserDetails("test")
  @Test
  void shouldReturnEducationDataWhenAuthorized() throws Exception {
    var mockAuthorizedClient = prepareOAuth2Client();
    var mockDataInJson =
        objectMapper.readTree(
            TestUtil.getContentFromFile(EDUCATIONS_HISTORY_KOSKI_RESPONSE, KoskiService.class));
    when(koskiOAuth2Service.fetchDataFromResourceServer(mockAuthorizedClient))
        .thenReturn(mockDataInJson);
    when(koskiService.getKoulutusData(mockDataInJson, null))
        .thenReturn(
            List.of(
                new KoulutusDto(
                    null, // id
                    ls(Kieli.FI, "nimi", Kieli.EN, "Name", Kieli.SV, "Namm"),
                    ls(Kieli.FI, "Kuvaus", Kieli.EN, "Description", Kieli.SV, "Beskrivning"),
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
    performGetEducationsDataFromKoski(status().isOk(), expectedResponseJson);

    verify(koskiOAuth2Service).fetchDataFromResourceServer(mockAuthorizedClient);
    verify(koskiService).getKoulutusData(mockDataInJson, null);
  }

  @Test
  @WithUserDetails("test")
  void shouldReturnForbidden_whenNotAuthorizedWithKoskiOAuth() throws Exception {
    var expectedResponseJson =
        """
        {"errorCode":"PERMISSION_REQUIRED","errorDetails":["Permission was not given or it is missing."]}
        """;
    performGetEducationsDataFromKoski(status().isForbidden(), expectedResponseJson);

    verify(koskiOAuth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verifyNoMoreInteractions(koskiOAuth2Service);
    verifyNoInteractions(koskiService);
  }

  private OAuth2AuthorizedClient prepareOAuth2Client() {
    var oAuth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
    when(koskiOAuth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(oAuth2AuthorizedClient);
    return oAuth2AuthorizedClient;
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnForbidden_whenTokenExpired() throws Exception {
    var oAuth2AuthorizedClient = prepareOAuth2Client();
    when(koskiOAuth2Service.fetchDataFromResourceServer(oAuth2AuthorizedClient))
        .thenThrow(new PermissionRequiredException("Token expired."));

    var expectedResponseJson =
        """
        {"errorCode":"PERMISSION_REQUIRED","errorDetails":["Token expired."]}
        """;
    performGetEducationsDataFromKoski(status().isForbidden(), expectedResponseJson);

    verify(koskiOAuth2Service).fetchDataFromResourceServer(oAuth2AuthorizedClient);
  }

  private void performGetEducationsDataFromKoski(
      ResultMatcher expectedResult, String expectedResponseJson) throws Exception {
    mockMvc
        .perform(get(API_KOSKI_KOULUTUKSET_ENDPOINT))
        .andExpect(expectedResult)
        .andExpect(content().json(expectedResponseJson));
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnInternalServerError_whenFetchingDataFails() throws Exception {
    var oAuth2AuthorizedClient = prepareOAuth2Client();
    when(koskiOAuth2Service.fetchDataFromResourceServer(oAuth2AuthorizedClient))
        .thenThrow(new ResourceServerException("Fail to get data from Koski resource server."));

    var expectedResponseJson =
        """
        {"errorCode":"SERVICE_ERROR","errorDetails":["Fail to get data from Koski resource server."]}
        """;
    performGetEducationsDataFromKoski(status().isInternalServerError(), expectedResponseJson);

    verify(koskiOAuth2Service).fetchDataFromResourceServer(oAuth2AuthorizedClient);
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnWrongPersonError_whenPersonalIdDoesNotMatch() throws Exception {
    var oAuth2AuthorizedClient = prepareOAuth2Client();
    when(koskiOAuth2Service.fetchDataFromResourceServer(oAuth2AuthorizedClient))
        .thenThrow(new WrongPersonException(UUID.randomUUID()));

    var expectedResponseJson =
        """
        {"errorCode":"WRONG_PERSON","errorDetails":["Wrong person."]}
        """;
    performGetEducationsDataFromKoski(status().isForbidden(), expectedResponseJson);

    verify(koskiOAuth2Service)
        .unauthorize(
            any(Authentication.class),
            any(HttpServletRequest.class),
            any(HttpServletResponse.class));
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnNoDataError_whenUserHaveNoDataInKoski() throws Exception {
    var oAuth2AuthorizedClient = prepareOAuth2Client();
    when(koskiOAuth2Service.fetchDataFromResourceServer(oAuth2AuthorizedClient))
        .thenThrow(
            new NoDataException(
                "omadataoauth2-error-94996a6c-a856-4dfd-8aee-da7edd578fe1: Oppijaa 1.2.246.562.24.51212001781 ei löydy tai käyttäjällä ei ole oikeuksia tietojen katseluun."));

    var expectedResponseJson =
        """
        {"errorCode":"DATA_NOT_FOUND","errorDetails":["The user either has no data or lacks access to retrieve it."]}
        """;
    performGetEducationsDataFromKoski(status().isForbidden(), expectedResponseJson);

    verify(koskiOAuth2Service).fetchDataFromResourceServer(oAuth2AuthorizedClient);
  }
}
