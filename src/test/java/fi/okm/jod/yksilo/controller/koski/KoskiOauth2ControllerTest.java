/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.koski;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.config.SessionLoginAttribute;
import fi.okm.jod.yksilo.config.koski.KoskiOauth2Config;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.koski.KoskiOauth2Service;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@TestPropertySource(properties = "jod.koski.enabled=true")
@Import({ErrorInfoFactory.class, KoskiOauth2Config.class, TestKoskiOauth2Config.class})
@WebMvcTest(KoskiOauth2Controller.class)
@Execution(ExecutionMode.SAME_THREAD)
class KoskiOauth2ControllerTest {

  private static final String REGISTRATION_ID = "koski";
  private static final String PERSON_ID = "241001B765F";

  private static final String AUTHORIZATION_URL = "/oauth2/authorization/koski";
  private static final String OAUTH2_CALLBACK_API_ENDPOINT = "/oauth2/response/koski";
  private static final String CALLBACK_PATH = "/koski/fi/omat-sivuni/osaamiseni/koulutukseni";
  private static final String EXPECTED_CALLBACK_URL_MISSING_REDIRECT = "?koski=missingCallback";
  private static final String EXPECTED_ERROR_REDIRECT = CALLBACK_PATH + "?koski=error";
  private static final String EXPECTED_CANCEL_REDIRECT = CALLBACK_PATH + "?koski=cancel";
  private static final String EXPECTED_AUTHORIZED_REDIRECT = CALLBACK_PATH + "?koski=authorized";

  @MockitoBean private KoskiOauth2Service koskiOauth2Service;

  @Autowired private MockMvc mockMvc;

  @Test
  @WithUserDetails(PERSON_ID)
  void shouldRedirectToOauth2Url_whenCallbackExists() throws Exception {
    when(koskiOauth2Service.getRegistrationId()).thenReturn(REGISTRATION_ID);

    var fullCallbackUrlWithParameters =
        "http://localhost:8080/koski/fi/omat-sivuni/osaamiseni/koulutukseni?callback="
            + CALLBACK_PATH
            + "&extra=blablabla";
    var result =
        mockMvc
            .perform(
                get("/oauth2/authorize/koski").param("callback", fullCallbackUrlWithParameters))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern(AUTHORIZATION_URL + "?locale=*"))
            .andReturn();

    var session = result.getRequest().getSession();
    assertThat(session).isNotNull();
    assertThat(session.getAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey()))
        .isEqualTo(CALLBACK_PATH);
  }

  @Test
  @WithUserDetails(PERSON_ID)
  void shouldNotRedirectToOauth2Url_whenCallbackIsMissing() throws Exception {
    mockMvc.perform(get("/oauth2/authorize/koski")).andExpect(status().isBadRequest());
  }

  @Test
  void shouldFail_whenUserIsNotAuthenticated() throws Exception {
    mockMvc.perform(get(OAUTH2_CALLBACK_API_ENDPOINT)).andExpect(status().isUnauthorized());
  }

  @Test
  @WithUserDetails(PERSON_ID)
  void shouldRedirectToLandingPage_whenUserGivesPermissionNoCallbackUrl() throws Exception {
    prepareOauth2Client();

    mockMvc
        .perform(get(OAUTH2_CALLBACK_API_ENDPOINT))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(EXPECTED_CALLBACK_URL_MISSING_REDIRECT));

    verifyNoInteractions(koskiOauth2Service);
  }

  @Test
  @WithUserDetails(PERSON_ID)
  void shouldRedirectToSavedUrlWithErrorCode_whenUserGivesPermissionButErrorOccurs()
      throws Exception {
    prepareOauth2Client();

    var queryMap =
        new LinkedMultiValueMap<>(
            Map.of(
                "error", List.of("invalid_token_response"),
                "error_description",
                    List.of(
                        "An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response: I/O error on POST request for...")));

    performCallback(EXPECTED_ERROR_REDIRECT, queryMap);

    verify(koskiOauth2Service, never())
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verifyNoMoreInteractions(koskiOauth2Service);
  }

  @Test
  @WithUserDetails(PERSON_ID)
  void shouldRedirectToSavedUrlWithCancelCode_whenUserDidNotGivePermission() throws Exception {
    prepareOauth2Client();
    when(koskiOauth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(null);

    performCallback(EXPECTED_CANCEL_REDIRECT);

    verify(koskiOauth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verifyNoMoreInteractions(koskiOauth2Service);
  }

  @Test
  @WithUserDetails(PERSON_ID)
  void shouldRedirectToSavedUrlWithOkAuthorizedCode_whenUserGivesPermission() throws Exception {
    prepareOauth2Client();
    performCallback(EXPECTED_AUTHORIZED_REDIRECT);

    verify(koskiOauth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verifyNoMoreInteractions(koskiOauth2Service);
  }

  private void performCallback(String expectedRedirectUrl) throws Exception {
    performCallback(expectedRedirectUrl, null);
  }

  private void performCallback(
      String expectedRedirectUrl, MultiValueMap<String, String> queryParameters) throws Exception {
    var requestBuilder = get(OAUTH2_CALLBACK_API_ENDPOINT);
    if (queryParameters != null && !queryParameters.isEmpty()) {
      requestBuilder = requestBuilder.params(queryParameters);
    }
    mockMvc
        .perform(
            requestBuilder.sessionAttr(
                SessionLoginAttribute.CALLBACK_FRONTEND.getKey(), CALLBACK_PATH))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(expectedRedirectUrl));
  }

  private void prepareOauth2Client() {
    var oauth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
    when(koskiOauth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(oauth2AuthorizedClient);
  }
}
