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
import fi.okm.jod.yksilo.domain.TuontiLahde;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
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
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import tools.jackson.databind.ObjectMapper;

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
  private static final String API_OSAAMISEN_TUNNISTUS_STATUS_QUERY =
      "/api/integraatiot/koski/osaamiset/tunnistus";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private KoskiOauth2Service koskiOauth2Service;

  @MockitoBean private KoskiService koskiService;

  @MockitoBean private KoulutusRepository koulutusRepository;

  @Autowired UserDetailsService userDetailsService;

  @BeforeEach
  void setUp() {
    when(koskiOauth2Service.fetchKoskiData(
            any(JodUser.class),
            any(Authentication.class),
            any(HttpServletRequest.class),
            any(HttpServletResponse.class)))
        .thenCallRealMethod();
  }

  @WithUserDetails("test")
  @Test
  void shouldReturnEducationDataWhenAuthorized() throws Exception {
    var mockAuthorizedClient = prepareOauth2Client();
    var mockDataInJson =
        objectMapper.readTree(
            TestUtil.getContentFromFile(EDUCATIONS_HISTORY_KOSKI_RESPONSE, KoskiService.class));
    when(koskiOauth2Service.fetchDataFromResourceServer(
            any(JodUser.class), eq(mockAuthorizedClient)))
        .thenReturn(mockDataInJson);
    when(koskiService.mapKoulutusKokonaisuudet(mockDataInJson)).thenCallRealMethod();
    when(koskiService.submit(any(JodUser.class), any()))
        .thenAnswer(
            inv ->
                new KoskiTehtavaDto(
                    UUID.randomUUID(),
                    KoskiTehtavaTila.VALMIS,
                    new KoskiTehtavaDto.Tulos(inv.getArgument(1))));

    mockMvc
        .perform(post(API_KOSKI_KOULUTUKSET_ENDPOINT).with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.tulos.koulutuskokonaisuudet.length()").value(2));
  }

  @Test
  @WithUserDetails("test")
  void shouldReturnForbidden_whenNotAuthorizedWithKoskiOauth() throws Exception {
    var expectedResponseJson =
        """
        {"errorCode":"PERMISSION_REQUIRED","errorDetails":["Permission was not given or it is missing."]}
        """;
    performCreateKoskiTehtava(status().isForbidden(), expectedResponseJson);

    verify(koskiOauth2Service)
        .fetchKoskiData(
            any(JodUser.class),
            any(Authentication.class),
            any(HttpServletRequest.class),
            any(HttpServletResponse.class));
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
    when(koskiOauth2Service.fetchDataFromResourceServer(
            any(JodUser.class), eq(oauth2AuthorizedClient)))
        .thenThrow(new PermissionRequiredException("Token expired."));

    var expectedResponseJson =
        """
        {"errorCode":"PERMISSION_REQUIRED","errorDetails":["Token expired."]}
        """;
    performCreateKoskiTehtava(status().isForbidden(), expectedResponseJson);
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
    var oauth2AuthorizedClient = prepareOauth2Client();
    when(koskiOauth2Service.fetchDataFromResourceServer(
            any(JodUser.class), eq(oauth2AuthorizedClient)))
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
    var client = prepareOauth2Client();
    when(koskiOauth2Service.fetchDataFromResourceServer(any(JodUser.class), eq(client)))
        .thenThrow(new WrongPersonException(UUID.randomUUID()));

    var expectedResponseJson =
        """
        {"errorCode":"WRONG_PERSON","errorDetails":["Wrong person."]}
        """;
    performCreateKoskiTehtava(status().isForbidden(), expectedResponseJson);

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
    when(koskiOauth2Service.fetchDataFromResourceServer(
            any(JodUser.class), eq(oauth2AuthorizedClient)))
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

  @WithUserDetails("test")
  @Test
  void shouldCreateKoskiTehtava_whenAuthorized() throws Exception {
    var authorizedClient = prepareOauth2Client();
    var mockDataInJson =
        objectMapper.readTree(
            TestUtil.getContentFromFile(EDUCATIONS_HISTORY_KOSKI_RESPONSE, KoskiService.class));
    when(koskiOauth2Service.fetchDataFromResourceServer(any(JodUser.class), eq(authorizedClient)))
        .thenReturn(mockDataInJson);

    var kokonaisuudet =
        List.of(
            new KoulutusKokonaisuusDto(
                UUID.randomUUID(),
                ls("Itä-Suomen yliopisto"),
                TuontiLahde.KOSKI_TUONTI,
                Set.of(
                    KoulutusDto.builder()
                        .id(UUID.randomUUID())
                        .nimi(ls("Lääketieteen lisensiaatti"))
                        .osaamisetOdottaaTunnistusta(true)
                        .build())));
    when(koskiService.mapKoulutusKokonaisuudet(mockDataInJson)).thenReturn(kokonaisuudet);
    var tehtavaId = UUID.randomUUID();
    var dto =
        new KoskiTehtavaDto(
            tehtavaId, KoskiTehtavaTila.VALMIS, new KoskiTehtavaDto.Tulos(kokonaisuudet));
    when(koskiService.submit(any(JodUser.class), eq(kokonaisuudet))).thenReturn(dto);

    mockMvc
        .perform(post(API_KOSKI_KOULUTUKSET_ENDPOINT).with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(tehtavaId.toString()))
        .andExpect(jsonPath("$.tila").value("VALMIS"))
        .andExpect(jsonPath("$.tulos.koulutuskokonaisuudet[0].tuontiLahde").value("KOSKI_TUONTI"));
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
          ],
          "skipOsaamistenTunnistus": false
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
