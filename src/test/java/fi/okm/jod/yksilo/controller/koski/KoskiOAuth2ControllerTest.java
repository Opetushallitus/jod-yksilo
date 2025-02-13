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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.config.SessionLoginAttribute;
import fi.okm.jod.yksilo.config.koski.KoskiOAuth2Config;
import fi.okm.jod.yksilo.config.koski.TestKoskiOAuth2Config;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.koski.KoskiOAuth2Service;
import fi.okm.jod.yksilo.util.UrlUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@TestPropertySource(
    properties = "spring.security.oauth2.client.registration.koski.provider=koski-mtls")
@Import({ErrorInfoFactory.class, KoskiOAuth2Config.class, TestKoskiOAuth2Config.class})
@ActiveProfiles("test")
@WebMvcTest(KoskiOAuth2Controller.class)
class KoskiOAuth2ControllerTest {

  private static final String REGISTRATION_ID = "koski";
  private static final String PERSON_ID = "241001B765F";

  private static final String AUTHORIZATION_URL = "/oauth2/authorization/koski";
  private static final String OAUTH2_CALLBACK_API_ENDPOINT = "/oauth2/response/koski";
  private static final String CALLBACK_PATH = "/koski/fi/omat-sivuni/osaamiseni/koulutukseni";
  private static final String EXPECTED_LANDING_PAGE_REDIRECT = "";
  private static final String EXPECTED_CALLBACK_URL_MISSING_REDIRECT = "?koski=missingCallback";
  private static final String EXPECTED_CANCEL_REDIRECT = CALLBACK_PATH + "?koski=cancel";
  private static final String EXPECTED_AUTHORIZED_REDIRECT = CALLBACK_PATH + "?koski=authorized";

  @MockitoBean private HttpServletRequest request;

  @MockitoBean private HttpSession session;

  @MockitoBean private KoskiOAuth2Service koskiOAuth2Service;

  @Autowired private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
  }

  @Test
  void shouldRedirectToOAuth2Url_whenCallbackExists() throws Exception {
    when(koskiOAuth2Service.getRegistrationId()).thenReturn(REGISTRATION_ID);

    var fullCallbackUrlWithParameters =
        "http://localhost:8080/koski/fi/omat-sivuni/osaamiseni/koulutukseni?callback="
            + CALLBACK_PATH
            + "&extra=blablabla";
    var result =
        mockMvc
            .perform(
                get("/oauth2/authorize/koski").param("callback", fullCallbackUrlWithParameters))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(AUTHORIZATION_URL))
            .andReturn();

    var session = result.getRequest().getSession();
    assertThat(session.getAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey()))
        .isEqualTo(UrlUtil.getRelativePath(CALLBACK_PATH));
  }

  @Test
  void shouldNotRedirectToOAuth2Url_whenCallbackIsMissing() throws Exception {
    mockMvc.perform(get("/oauth2/authorize/koski")).andExpect(status().isBadRequest());
  }

  @Test
  void shouldRedirectToLandingPage_whenUserGivesPermissionWithoutLogin() throws Exception {
    JodUser jodUser = null;
    var oAuth2AuthorizedClient = prepareOAuth2Client();

    authenticateUser(jodUser);

    performCallback(oAuth2AuthorizedClient, EXPECTED_LANDING_PAGE_REDIRECT);

    verifyNoInteractions(koskiOAuth2Service);
  }

  @Test
  void shouldRedirectToLandingPage_whenUserGivesPermissionNoCallbackUrl() throws Exception {
    var jodUser = mock(JodUser.class);
    when(jodUser.getPersonId()).thenReturn(PERSON_ID);
    var oAuth2AuthorizedClient = prepareOAuth2Client();

    authenticateUser(jodUser);

    mockMvc
        .perform(
            get(OAUTH2_CALLBACK_API_ENDPOINT)
                .principal(new TestingAuthenticationToken(oAuth2AuthorizedClient, null)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(EXPECTED_CALLBACK_URL_MISSING_REDIRECT));

    verifyNoInteractions(koskiOAuth2Service);
  }

  @Test
  void shouldTriggerCallbackEndpoint_whenUserDidNotGivePermission() throws Exception {
    var jodUser = mock(JodUser.class);
    when(jodUser.getPersonId()).thenReturn(PERSON_ID);

    var oAuth2AuthorizedClient = prepareOAuth2Client();
    when(koskiOAuth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(null);

    authenticateUser(jodUser);

    performCallback(oAuth2AuthorizedClient, EXPECTED_CANCEL_REDIRECT);

    verify(koskiOAuth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verifyNoMoreInteractions(koskiOAuth2Service);
  }

  @Test
  void shouldTriggerCallbackEndpoint_whenUserGivesPermission() throws Exception {
    var jodUser = mock(JodUser.class);
    when(jodUser.getPersonId()).thenReturn(PERSON_ID);

    var oAuth2AuthorizedClient = prepareOAuth2Client();

    authenticateUser(jodUser);

    performCallback(oAuth2AuthorizedClient, EXPECTED_AUTHORIZED_REDIRECT);

    verify(koskiOAuth2Service)
        .getAuthorizedClient(any(Authentication.class), any(HttpServletRequest.class));
    verifyNoMoreInteractions(koskiOAuth2Service);
  }

  private void performCallback(
      OAuth2AuthorizedClient oAuth2AuthorizedClient, String expectedRedirectUrl) throws Exception {
    mockMvc
        .perform(
            get(OAUTH2_CALLBACK_API_ENDPOINT)
                .sessionAttr(SessionLoginAttribute.CALLBACK_FRONTEND.getKey(), CALLBACK_PATH)
                .principal(new TestingAuthenticationToken(oAuth2AuthorizedClient, null)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(expectedRedirectUrl));
  }

  private OAuth2AuthorizedClient prepareOAuth2Client() {
    var clientRegistration = mock(ClientRegistration.class);
    when(clientRegistration.getRegistrationId()).thenReturn(REGISTRATION_ID);

    var oAuth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
    when(oAuth2AuthorizedClient.getClientRegistration()).thenReturn(clientRegistration);
    when(koskiOAuth2Service.getAuthorizedClient(
            any(Authentication.class), any(HttpServletRequest.class)))
        .thenReturn(oAuth2AuthorizedClient);

    return oAuth2AuthorizedClient;
  }

  private static void authenticateUser(JodUser jodUser) {
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new TestingAuthenticationToken(jodUser, null));
    SecurityContextHolder.setContext(context);
  }
}
