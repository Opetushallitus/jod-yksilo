/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.koski;

import static fi.okm.jod.yksilo.config.mocklogin.MockJodUserImpl.ROLE_FULL_USER;
import static fi.okm.jod.yksilo.config.mocklogin.MockJodUserImpl.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.config.SecurityConfig;
import fi.okm.jod.yksilo.config.SessionLoginAttribute;
import fi.okm.jod.yksilo.config.koski.KoskiOauth2Config;
import fi.okm.jod.yksilo.config.koski.KoskiRestClientConfig;
import fi.okm.jod.yksilo.config.koski.KoskiSecurityConfig;
import fi.okm.jod.yksilo.config.mocklogin.MockJodUserImpl;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.koski.KoskiService;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/** Runs the Koski OAuth2 authorization code flow through the real Koski security filter chain. */
@TestPropertySource(properties = "jod.koski.enabled=true")
@Import({
  ErrorInfoFactory.class,
  SecurityConfig.class,
  KoskiOauth2Config.class,
  KoskiSecurityConfig.class,
  KoskiOauth2FlowTest.Config.class
})
@WebMvcTest(IntegraatioKoskiController.class)
@Execution(ExecutionMode.SAME_THREAD)
class KoskiOauth2FlowTest {

  private static final String AUTHORIZATION_URI = "https://koski.test/omadata-oauth2/authorize";
  private static final String TOKEN_URI = "https://koski.test/omadata-oauth2/token";
  private static final String RESPONSE_URL = "http://localhost/oauth2/response/koski";
  private static final String FRONTEND_PATH = "/yksilo/fi/omat-sivuni/osaamiseni/koulutukseni";

  @Autowired private MockMvc mockMvc;
  @Autowired private MockRestServiceServer koskiServer;

  @MockitoBean private KoskiService koskiService;

  private final MockHttpSession session = new MockHttpSession();

  @BeforeEach
  void resetKoskiServer() {
    koskiServer.reset();
  }

  @Test
  @WithUserDetails("full")
  void shouldRedirectToKoskiWithPkceFormPostAndLocale() throws Exception {
    var authorizationRequest = authorize();

    assertThat(authorizationRequest.toUriString()).startsWith(AUTHORIZATION_URI + "?");
    var params = authorizationRequest.getQueryParams();
    assertThat(params.getFirst("response_mode")).isEqualTo("form_post");
    assertThat(params.getFirst("locale")).isEqualTo("sv");
    assertThat(params.getFirst("code_challenge_method")).isEqualTo("S256");
    assertThat(params.getFirst("code_challenge")).isNotBlank();
    assertThat(params.getFirst("redirect_uri")).isEqualTo(RESPONSE_URL);
    assertThat(session.getAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey()))
        .isEqualTo(FRONTEND_PATH);
  }

  @Test
  @WithUserDetails("full")
  void shouldReturnToFrontendAsAuthorized_whenKoskiGrantsAccess() throws Exception {
    var state = authorizeAndGetState();
    koskiServer
        .expect(requestTo(TOKEN_URI))
        .andRespond(
            withSuccess(
                """
                {"access_token":"koski-token","token_type":"Bearer","expires_in":300}
                """,
                MediaType.APPLICATION_JSON));

    mockMvc
        .perform(formPostResponse().param("state", state).param("code", "authorization-code"))
        .andExpect(redirectedUrl(RESPONSE_URL));
    koskiServer.verify();

    mockMvc
        .perform(get("/oauth2/response/koski").session(session))
        .andExpect(status().isSeeOther())
        .andExpect(redirectedUrl(FRONTEND_PATH + "?koski=authorized"));
    assertThat(session.getAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey())).isNull();
  }

  @Test
  @WithUserDetails("full")
  void shouldReturnToFrontendAsCancelled_whenUserDeniesAccessInKoski() throws Exception {
    var state = authorizeAndGetState();

    var errorRedirect =
        mockMvc
            .perform(formPostResponse().param("state", state).param("error", "access_denied"))
            .andExpect(status().is3xxRedirection())
            .andReturn()
            .getResponse()
            .getRedirectedUrl();

    mockMvc
        .perform(get(errorRedirect).session(session))
        .andExpect(status().isSeeOther())
        .andExpect(redirectedUrl(FRONTEND_PATH + "?koski=cancel"));
  }

  @Test
  @WithUserDetails("full")
  void shouldReturnToFrontendAsErrorWithoutDescription_whenTokenExchangeFails() throws Exception {
    var state = authorizeAndGetState();
    koskiServer
        .expect(requestTo(TOKEN_URI))
        .andRespond(
            withBadRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                    """
                    {"error":"invalid_grant","error_description":"Oppija 1.2.246.562.24.00000000001"}
                    """));

    var errorRedirect =
        mockMvc
            .perform(formPostResponse().param("state", state).param("code", "authorization-code"))
            .andExpect(status().is3xxRedirection())
            .andReturn()
            .getResponse()
            .getRedirectedUrl();
    assertThat(errorRedirect).isEqualTo(RESPONSE_URL + "?error=invalid_grant");

    mockMvc
        .perform(get(errorRedirect).session(session))
        .andExpect(status().isSeeOther())
        .andExpect(redirectedUrl(FRONTEND_PATH + "?koski=error"));
  }

  @Test
  @WithUserDetails("full")
  void shouldReturnToFrontendAsError_whenResponseEndpointIsOpenedWithoutAuthorization()
      throws Exception {
    authorize();

    mockMvc
        .perform(get("/oauth2/response/koski").session(session))
        .andExpect(status().isSeeOther())
        .andExpect(redirectedUrl(FRONTEND_PATH + "?koski=error"));
  }

  @Test
  @WithUserDetails("full")
  void shouldReturnToRoot_whenNoCallbackWasSaved() throws Exception {
    mockMvc
        .perform(get("/oauth2/response/koski").session(session))
        .andExpect(status().isSeeOther())
        .andExpect(redirectedUrl("/?koski=error"));
  }

  @Test
  @WithUserDetails("full")
  void shouldReturnToFrontendAsError_whenResponseStateDoesNotMatchPendingAuthorization()
      throws Exception {
    authorize();

    mockMvc
        .perform(formPostResponse().param("state", "stale").param("code", "authorization-code"))
        .andExpect(status().isSeeOther())
        .andExpect(redirectedUrl(FRONTEND_PATH + "?koski=error"));
  }

  private UriComponents authorize() throws Exception {
    var location =
        mockMvc
            .perform(
                get("/oauth2/authorization/koski")
                    .session(session)
                    .param("callback", "http://localhost" + FRONTEND_PATH + "?foo=bar")
                    .param("lang", "sv"))
            .andExpect(status().is3xxRedirection())
            .andReturn()
            .getResponse()
            .getRedirectedUrl();
    return UriComponentsBuilder.fromUriString(location).build();
  }

  /** The browser decodes the state before Koski posts it back, so the test must do the same. */
  private String authorizeAndGetState() throws Exception {
    return URLDecoder.decode(
        authorize().getQueryParams().getFirst("state"), StandardCharsets.UTF_8);
  }

  /** Koski sends the authorization response as a cross-site form POST, without a CSRF token. */
  private MockHttpServletRequestBuilder formPostResponse() {
    return post("/oauth2/response/koski")
        .session(session)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED);
  }

  @TestConfiguration
  static class Config {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
      return new InMemoryClientRegistrationRepository(
          ClientRegistration.withRegistrationId("koski")
              .clientId("jod")
              .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
              .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
              .redirectUri("{baseUrl}/oauth2/response/{registrationId}")
              .scope("HENKILOTIEDOT_HETU", "OPISKELUOIKEUDET_KAIKKI_TIEDOT")
              .authorizationUri(AUTHORIZATION_URI)
              .tokenUri(TOKEN_URI)
              .build());
    }

    @Bean
    RestClient.Builder koskiOauth2RestClientBuilder() {
      return RestClient.builder()
          .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
          .configureMessageConverters(
              converters ->
                  converters
                      .addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter())
                      .addCustomConverter(new FormHttpMessageConverter()));
    }

    @Bean
    MockRestServiceServer koskiServer(RestClient.Builder koskiOauth2RestClientBuilder) {
      return MockRestServiceServer.bindTo(koskiOauth2RestClientBuilder).build();
    }

    @Bean(KoskiRestClientConfig.OAUTH2_RESTCLIENT_ID)
    RestClient koskiOauth2RestClient(
        RestClient.Builder koskiOauth2RestClientBuilder, MockRestServiceServer koskiServer) {
      return koskiOauth2RestClientBuilder.build();
    }

    @Bean
    UserDetailsService userDetailsService() {
      return username ->
          "full".equals(username)
              ? new MockJodUserImpl(
                  username, UUID.nameUUIDFromBytes(username.getBytes()), ROLE_FULL_USER, ROLE_USER)
              : new MockJodUserImpl(username, UUID.nameUUIDFromBytes(username.getBytes()));
    }
  }
}
