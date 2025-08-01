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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.okm.jod.yksilo.config.koski.KoskiOauth2Config;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.repository.KoulutusRepository;
import fi.okm.jod.yksilo.service.koski.KoskiOauth2Service;
import fi.okm.jod.yksilo.service.koski.KoskiService;
import fi.okm.jod.yksilo.service.koski.NoDataException;
import fi.okm.jod.yksilo.service.koski.PermissionRequiredException;
import fi.okm.jod.yksilo.service.koski.ResourceServerException;
import fi.okm.jod.yksilo.service.koski.WrongPersonException;
import fi.okm.jod.yksilo.testutil.TestUtil;
import fi.okm.jod.yksilo.validation.Limits;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

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

  private static final String EDUCATIONS_HISTORY_KOSKI_RESPONSE = "koski-response.json";
  private static final String API_KOSKI_KOULUTUKSET_ENDPOINT =
      "/api/integraatiot/koski/koulutukset";
  private static final String GET_EDUCATIONS_DATA_API_RESPONSE =
      "getEducationsDataFromKoski-response.json";
  private static final String API_OSAAMISEN_TUNNISTUS_STATUS_QUERY =
      "/api/integraatiot/koski/osaamiset/tunnistus";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private KoskiOauth2Service koskiOauth2Service;

  @MockitoBean private KoskiService koskiService;

  @MockitoBean private KoulutusRepository koulutusRepository;

  @Autowired UserDetailsService userDetailsService;

  @WithUserDetails("test")
  @Test
  void shouldReturnEducationDataWhenAuthorized() throws Exception {
    var mockAuthorizedClient = prepareOauth2Client();
    var mockDataInJson =
        objectMapper.readTree(
            TestUtil.getContentFromFile(EDUCATIONS_HISTORY_KOSKI_RESPONSE, KoskiService.class));
    when(koskiOauth2Service.fetchDataFromResourceServer(mockAuthorizedClient))
        .thenReturn(mockDataInJson);
    doAnswer(invocationOnMock -> null)
        .when(koskiOauth2Service)
        .checkPersonIdMatches(any(JodUser.class), any(JsonNode.class));
    when(koskiService.getKoulutusData(mockDataInJson)).thenCallRealMethod();

    var expectedResponseJson =
        TestUtil.getContentFromFile(GET_EDUCATIONS_DATA_API_RESPONSE, KoskiService.class);
    performGetEducationsDataFromKoski(status().isOk(), expectedResponseJson);

    verify(koskiOauth2Service).fetchDataFromResourceServer(mockAuthorizedClient);
    verify(koskiService).getKoulutusData(mockDataInJson);
  }

  @Test
  @WithUserDetails("test")
  void shouldReturnForbidden_whenNotAuthorizedWithKoskiOauth() throws Exception {
    var expectedResponseJson =
        """
        {"errorCode":"PERMISSION_REQUIRED","errorDetails":["Permission was not given or it is missing."]}
        """;
    performGetEducationsDataFromKoski(status().isForbidden(), expectedResponseJson);

    verify(koskiOauth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verifyNoMoreInteractions(koskiOauth2Service);
    verifyNoInteractions(koskiService);
  }

  private OAuth2AuthorizedClient prepareOauth2Client() {
    var oauth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
    when(koskiOauth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(oauth2AuthorizedClient);
    return oauth2AuthorizedClient;
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnForbidden_whenTokenExpired() throws Exception {
    var oauth2AuthorizedClient = prepareOauth2Client();
    when(koskiOauth2Service.fetchDataFromResourceServer(oauth2AuthorizedClient))
        .thenThrow(new PermissionRequiredException("Token expired."));

    var expectedResponseJson =
        """
        {"errorCode":"PERMISSION_REQUIRED","errorDetails":["Token expired."]}
        """;
    performGetEducationsDataFromKoski(status().isForbidden(), expectedResponseJson);

    verify(koskiOauth2Service).fetchDataFromResourceServer(oauth2AuthorizedClient);
  }

  private void performGetEducationsDataFromKoski(
      ResultMatcher expectedResult, String expectedResponseJson) throws Exception {
    mockMvc
        .perform(get(API_KOSKI_KOULUTUKSET_ENDPOINT))
        .andDo(MockMvcResultHandlers.print())
        .andExpect(expectedResult)
        .andExpect(content().json(expectedResponseJson));
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnInternalServerError_whenFetchingDataFails() throws Exception {
    var oauth2AuthorizedClient = prepareOauth2Client();
    when(koskiOauth2Service.fetchDataFromResourceServer(oauth2AuthorizedClient))
        .thenThrow(new ResourceServerException("Fail to get data from Koski resource server."));

    var expectedResponseJson =
        """
        {"errorCode":"SERVICE_ERROR","errorDetails":["Fail to get data from Koski resource server."]}
        """;
    performGetEducationsDataFromKoski(status().isInternalServerError(), expectedResponseJson);

    verify(koskiOauth2Service).fetchDataFromResourceServer(oauth2AuthorizedClient);
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnWrongPersonError_whenPersonalIdDoesNotMatch() throws Exception {
    var oauth2AuthorizedClient = prepareOauth2Client();
    when(koskiOauth2Service.fetchDataFromResourceServer(oauth2AuthorizedClient))
        .thenThrow(new WrongPersonException(UUID.randomUUID()));

    var expectedResponseJson =
        """
        {"errorCode":"WRONG_PERSON","errorDetails":["Wrong person."]}
        """;
    performGetEducationsDataFromKoski(status().isForbidden(), expectedResponseJson);

    verify(koskiOauth2Service)
        .unauthorize(
            any(Authentication.class),
            any(HttpServletRequest.class),
            any(HttpServletResponse.class));
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnNoDataError_whenUserHaveNoDataInKoski() throws Exception {
    var oauth2AuthorizedClient = prepareOauth2Client();
    when(koskiOauth2Service.fetchDataFromResourceServer(oauth2AuthorizedClient))
        .thenThrow(
            new NoDataException(
                "omadataoauth2-error-94996a6c-a856-4dfd-8aee-da7edd578fe1: Oppijaa 1.2.246.562.24.51212001781 ei löydy tai käyttäjällä ei ole oikeuksia tietojen katseluun."));

    var expectedResponseJson =
        """
        {"errorCode":"DATA_NOT_FOUND","errorDetails":["The user either has no data or lacks access to retrieve it."]}
        """;
    performGetEducationsDataFromKoski(status().isForbidden(), expectedResponseJson);

    verify(koskiOauth2Service).fetchDataFromResourceServer(oauth2AuthorizedClient);
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnOsaamisetIdentified_whenValidRequestProvided() throws Exception {
    var koulutusUuids =
        List.of(
            UUID.fromString("5edaca37-8ca1-4f18-918b-7aa73997c676"),
            UUID.fromString("842a5528-06ac-455a-8d7e-7a401947b1f7"));

    var koulutus1 =
        KoulutusDto.builder()
            .id(koulutusUuids.get(0))
            .nimi(ls("koulutus1"))
            .osaamiset(
                Set.of(
                    URI.create(
                        "http://data.europa.eu/esco/skill/008fa98b-dba6-4abf-909e-04299728e3eb")))
            .osaamisetOdottaaTunnistusta(true)
            .osaamisetTunnistusEpaonnistui(false)
            .build();
    var koulutus2 =
        KoulutusDto.builder()
            .id(koulutusUuids.get(1))
            .nimi(ls("koulutus2"))
            .osaamiset(Collections.emptySet())
            .osaamisetOdottaaTunnistusta(false)
            .osaamisetTunnistusEpaonnistui(true)
            .build();

    when(koskiService.getOsaamisetIdentified(any(JodUser.class), eq(koulutusUuids)))
        .thenReturn(List.of(koulutus1, koulutus2));

    mockMvc
        .perform(
            get(API_OSAAMISEN_TUNNISTUS_STATUS_QUERY)
                .param("ids", koulutusUuids.get(0).toString())
                .param("ids", koulutusUuids.get(1).toString())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(koulutus1.id().toString()))
        .andExpect(
            jsonPath("$[0].osaamiset[0]").value(koulutus1.osaamiset().iterator().next().toString()))
        .andExpect(
            jsonPath("$[0].osaamisetOdottaaTunnistusta")
                .value(koulutus1.osaamisetOdottaaTunnistusta()))
        .andExpect(
            jsonPath("$[0].osaamisetTunnistusEpaonnistui")
                .value(koulutus1.osaamisetTunnistusEpaonnistui()))
        .andExpect(jsonPath("$[1].id").value(koulutus2.id().toString()))
        .andExpect(jsonPath("$[1].osaamiset").isEmpty())
        .andExpect(
            jsonPath("$[1].osaamisetOdottaaTunnistusta")
                .value(koulutus2.osaamisetOdottaaTunnistusta()))
        .andExpect(
            jsonPath("$[1].osaamisetTunnistusEpaonnistui")
                .value(koulutus2.osaamisetTunnistusEpaonnistui()));
  }

  @Test
  @WithUserDetails("test")
  void shouldReturnBadRequest_whenNoIdsProvided() throws Exception {
    mockMvc
        .perform(get(API_OSAAMISEN_TUNNISTUS_STATUS_QUERY).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("test")
  void shouldReturnBadRequest_whenExceedsUuidLimit() throws Exception {
    var maxAllowedUuids = Limits.KOULUTUSKOKONAISUUS;
    var commaSeparatedUuids =
        StringUtils.join(
            ',', Stream.generate(UUID::randomUUID).limit(maxAllowedUuids + 1).toList());

    mockMvc
        .perform(
            get(API_OSAAMISEN_TUNNISTUS_STATUS_QUERY)
                .contentType(MediaType.APPLICATION_JSON)
                .param("ids", commaSeparatedUuids))
        .andExpect(status().isBadRequest());
  }
}
