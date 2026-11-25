/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

class Oauth2CallbackFilterTest {

  private static final String CALLBACK_KEY = SessionLoginAttribute.CALLBACK_FRONTEND.getKey();

  private final Oauth2CallbackFilter filter = new Oauth2CallbackFilter("koski");

  @Test
  void shouldSaveOnlyNormalizedCallbackPath_whenAuthorizationRequested() throws Exception {
    var session = new MockHttpSession();
    var request = new MockHttpServletRequest("GET", "/yksilo/oauth2/authorization/koski");
    request.setContextPath("/yksilo");
    request.setSession(session);
    request.setParameter(
        "callback", "https://evil.example/yksilo/fi/omat-sivuni/../omat-sivuni/koulutukseni?x=1");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(session.getAttribute(CALLBACK_KEY)).isEqualTo("/yksilo/fi/omat-sivuni/koulutukseni");
  }

  @Test
  void shouldNotSaveCallback_whenRequestIsForAnotherRegistration() throws Exception {
    var session = new MockHttpSession();
    var request = new MockHttpServletRequest("GET", "/yksilo/oauth2/authorization/tmt-haku");
    request.setContextPath("/yksilo");
    request.setSession(session);
    request.setParameter("callback", "/yksilo/fi/omat-sivuni");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertThat(session.getAttribute(CALLBACK_KEY)).isNull();
  }

  @Test
  void shouldContinueFilterChain() throws Exception {
    var request = new MockHttpServletRequest("GET", "/oauth2/authorization/koski");
    var chain = new MockFilterChain();

    filter.doFilter(request, new MockHttpServletResponse(), chain);

    assertThat(chain.getRequest()).isSameAs(request);
  }
}
