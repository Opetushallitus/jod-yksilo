/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.koski;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.config.SessionLoginAttribute;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.util.UrlUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Objects;

@TestPropertySource(
    properties = "spring.security.oauth2.client.registration.koski.provider=koski-mtls")
@Import({
  ErrorInfoFactory.class,
  MappingConfig.class,
  KoskiOAuth2Config.class,
  TestKoskiOAuth2Config.class
})
@ActiveProfiles("test")
@WebMvcTest(KoskiOAuth2Controller.class)
public class KoskiOAuth2ControllerTest {

  @MockitoBean private KoskiOAuth2Service koskiOAuth2Service;

  @Autowired private MockMvc mockMvc;

  @Autowired private WebApplicationContext webApplicationContext;

  @BeforeEach
  public void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext).build();
  }

  @Test
  public void testRedirect() throws Exception {
    var callback = "/koski/fi/omat-sivuni/osaamiseni/koulutukseni";
    var callbackUrl = UrlUtil.getRelativePath(callback);
    var authorizationUrl = "/oauth2/authorization/koski";
    when(koskiOAuth2Service.getRegistrationId()).thenReturn("koski");

    mockMvc
        .perform(get("/oauth2/authorize/koski").param("callback", callback))
        .andDo(print())
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(authorizationUrl));

    var session =
        mockMvc
            .perform(get("/oauth2/authorize/koski").param("callback", callback))
            .andReturn()
            .getRequest()
            .getSession();

    assert Objects.requireNonNull(session)
        .getAttribute(SessionLoginAttribute.CALLBACK_FRONTEND.getKey())
        .equals(callbackUrl);
  }
}
