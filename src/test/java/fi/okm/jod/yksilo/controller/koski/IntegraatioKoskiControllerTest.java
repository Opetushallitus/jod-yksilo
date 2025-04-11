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
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.OsaamisenTunnistusStatus;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.repository.KoulutusRepository;
import fi.okm.jod.yksilo.service.koski.KoskiOAuth2Service;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

@TestPropertySource(properties = "jod.koski.enabled=true")
@Import({
  ErrorInfoFactory.class,
  KoskiOAuth2Config.class,
  TestKoskiOAuth2Config.class,
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

  @MockitoBean private KoskiOAuth2Service koskiOAuth2Service;

  @MockitoSpyBean private KoskiService koskiService;

  @MockitoBean private KoulutusRepository koulutusRepository;

  @Autowired UserDetailsService userDetailsService;

  @WithUserDetails("test")
  @Test
  void shouldReturnEducationDataWhenAuthorized() throws Exception {
    var mockAuthorizedClient = prepareOAuth2Client();
    var mockDataInJson =
        objectMapper.readTree(
            TestUtil.getContentFromFile(EDUCATIONS_HISTORY_KOSKI_RESPONSE, KoskiService.class));
    when(koskiOAuth2Service.fetchDataFromResourceServer(mockAuthorizedClient))
        .thenReturn(mockDataInJson);
    doAnswer(invocationOnMock -> null)
        .when(koskiOAuth2Service)
        .checkPersonIdMatches(any(JodUser.class), any(JsonNode.class));

    var expectedResponseJson =
        TestUtil.getContentFromFile(GET_EDUCATIONS_DATA_API_RESPONSE, KoskiService.class);
    performGetEducationsDataFromKoski(status().isOk(), expectedResponseJson);

    verify(koskiOAuth2Service).fetchDataFromResourceServer(mockAuthorizedClient);
    verify(koskiService).getKoulutusData(mockDataInJson);
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

  @WithUserDetails("test")
  @Test
  void shouldReturnOsaamisetIdentified_whenValidRequestProvided() throws Exception {
    var koulutusUUIDs =
        List.of(
            UUID.fromString("5edaca37-8ca1-4f18-918b-7aa73997c676"),
            UUID.fromString("842a5528-06ac-455a-8d7e-7a401947b1f7"));
    var yksilo = new Yksilo(UUID.nameUUIDFromBytes("test".getBytes()));

    var koulutus1 = mock(Koulutus.class);
    when(koulutus1.getYksilo()).thenReturn(yksilo);
    when(koulutus1.getId()).thenReturn(koulutusUUIDs.get(0));
    when(koulutus1.getNimi()).thenReturn(new LocalizedString(Map.of(Kieli.FI, "koulutus1")));
    when(koulutus1.getAlkuPvm()).thenReturn(LocalDate.of(2023, 1, 1));
    when(koulutus1.getLoppuPvm()).thenReturn(LocalDate.of(2023, 12, 31));
    when(koulutus1.getOsaamisenTunnistusStatus()).thenReturn(OsaamisenTunnistusStatus.DONE);
    var yksilonOsaaminen1 = mock(YksilonOsaaminen.class);
    var osaaminen1 = mock(Osaaminen.class);
    when(osaaminen1.getUri())
        .thenReturn(
            URI.create("http://data.europa.eu/esco/skill/008fa98b-dba6-4abf-909e-04299728e3eb"));
    when(yksilonOsaaminen1.getOsaaminen()).thenReturn(osaaminen1);
    when(koulutus1.getOsaamiset()).thenReturn(Set.of(yksilonOsaaminen1));
    var koulutus2 = mock(Koulutus.class);
    when(koulutus2.getYksilo()).thenReturn(yksilo);
    when(koulutus2.getId()).thenReturn(koulutusUUIDs.get(1));
    when(koulutus2.getNimi()).thenReturn(new LocalizedString(Map.of(Kieli.FI, "koulutus2")));
    when(koulutus2.getAlkuPvm()).thenReturn(LocalDate.of(2024, 1, 1));
    when(koulutus2.getOsaamisenTunnistusStatus()).thenReturn(OsaamisenTunnistusStatus.WAIT);
    when(koulutusRepository.findByKokonaisuusYksiloIdAndIdIn(yksilo.getId(), koulutusUUIDs))
        .thenReturn(List.of(koulutus1, koulutus2));

    var expectedJson =
        """
            [
                {
                    "id": "5edaca37-8ca1-4f18-918b-7aa73997c676",
                    "nimi": {
                        "fi": "koulutus1"
                    },
                    "alkuPvm": "2023-01-01",
                    "loppuPvm": "2023-12-31",
                    "osaamiset": [
                        "http://data.europa.eu/esco/skill/008fa98b-dba6-4abf-909e-04299728e3eb"
                    ],
                    "osaamisetOdottaaTunnistusta": false,
                    "osaamisetTunnistusEpaonnistui": false,
                    "osasuoritukset": []
                },
                {
                    "id": "842a5528-06ac-455a-8d7e-7a401947b1f7",
                    "nimi": {
                        "fi": "koulutus2"
                    },
                    "alkuPvm": "2024-01-01",
                    "osaamiset": [],
                    "osaamisetOdottaaTunnistusta": true,
                    "osaamisetTunnistusEpaonnistui": false,
                    "osasuoritukset": []
                }
            ]
            """;
    mockMvc
        .perform(
            get(API_OSAAMISEN_TUNNISTUS_STATUS_QUERY)
                .param("ids", koulutusUUIDs.get(0).toString())
                .param("ids", koulutusUUIDs.get(1).toString())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedJson));
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
    var maxAllowedUuids = Limits.KOULUTUSKOKONAISUUS * Limits.KOULUTUS_PER_KOKONAISUUS;
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
