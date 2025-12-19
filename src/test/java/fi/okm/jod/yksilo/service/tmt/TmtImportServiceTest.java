/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.tmt;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import fi.okm.jod.yksilo.config.tmt.TmtConfiguration;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.external.tmt.model.EmploymentDtoExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.FullProfileDtoExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.IntervalItemExternalGet;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import fi.okm.jod.yksilo.service.OsaaminenService;
import fi.okm.jod.yksilo.service.profiili.KoulutusKokonaisuusService;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import fi.okm.jod.yksilo.service.profiili.PatevyysService;
import fi.okm.jod.yksilo.service.profiili.ToimenkuvaService;
import fi.okm.jod.yksilo.service.profiili.ToimintoService;
import fi.okm.jod.yksilo.service.profiili.TyopaikkaService;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestClient;

@TestPropertySource(
    properties = {
      "jod.tmt.enabled=true",
      "jod.tmt.import-api.kipa-subscription-key=key",
      "jod.tmt.import-api.api-url=/v1/profile",
    })
@Slf4j
class TmtImportServiceTest extends AbstractServiceTest {
  @Autowired private TmtImportService tmtImportService;
  @Autowired private TestConfig testConfig;
  @Autowired private MappingJackson2HttpMessageConverter converter;
  @Autowired private TyopaikkaService tyopaikkaService;

  @TestConfiguration
  @Import({
    TmtImportService.class,
    TmtImportMapper.class,
    TmtConfiguration.class,
    OsaaminenService.class,
    LocalValidatorFactoryBean.class,
    TyopaikkaService.class,
    ToimenkuvaService.class,
    KoulutusKokonaisuusService.class,
    KoulutusService.class,
    ToimintoService.class,
    PatevyysService.class,
    YksilonOsaaminenService.class
  })
  static class TestConfig {
    private final RestClient.Builder clientBuilder = RestClient.builder();
    private final MockRestServiceServer mockServer =
        MockRestServiceServer.bindTo(clientBuilder).build();

    @Bean
    RestClient tmtRestClient() {
      return clientBuilder.build();
    }

    @Bean
    MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
      return new MappingJackson2HttpMessageConverter();
    }
  }

  @BeforeEach
  void setUp() {
    testConfig.mockServer.reset();
  }

  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  void shouldImportProfile() throws JsonProcessingException {

    var profile = new FullProfileDtoExternalGet();
    profile.employments(
        List.of(
            new EmploymentDtoExternalGet()
                .interval(new IntervalItemExternalGet().startDate(LocalDate.now()).ongoing(true))
                .title(Map.of(Kieli.FI.getKoodi(), "Työnimike"))
                .employer(Map.of(Kieli.FI.getKoodi(), "Työnantaja"))));

    testConfig
        .mockServer
        .expect(requestTo("/v1/profile"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer token"))
        .andExpect(header("KIPA-Subscription-Key", "key"))
        .andRespond(
            withSuccess(
                converter.getObjectMapper().writeValueAsString(profile),
                MediaType.APPLICATION_JSON));

    var accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, "token", Instant.now(), Instant.MAX);

    var result = assertDoesNotThrow(() -> tmtImportService.importProfile(user, accessToken));
    simulateCommit();

    assertThat(result.tyopaikat()).hasSize(1);
    assertThat(tyopaikkaService.get(user, result.tyopaikat().iterator().next()).nimi())
        .isEqualTo(ls("Työnantaja"));
  }

  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  void shouldIgnoreInvalidData() throws JsonProcessingException {

    var profile = new FullProfileDtoExternalGet();
    profile.employments(
        List.of(
            new EmploymentDtoExternalGet(),
            new EmploymentDtoExternalGet()
                .interval(new IntervalItemExternalGet().startDate(LocalDate.now()).ongoing(true))
                .title(Map.of(Kieli.EN.getKoodi(), "VALID"))
                .employer(Map.of(Kieli.EN.getKoodi(), "VALID"))));

    testConfig
        .mockServer
        .expect(requestTo("/v1/profile"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer token"))
        .andExpect(header("KIPA-Subscription-Key", "key"))
        .andRespond(
            withSuccess(
                converter.getObjectMapper().writeValueAsString(profile),
                MediaType.APPLICATION_JSON));

    var accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, "token", Instant.now(), Instant.MAX);

    var result = assertDoesNotThrow(() -> tmtImportService.importProfile(user, accessToken));
    assertThat(result.tyopaikat()).hasSize(1);
    assertThat(tyopaikkaService.get(user, result.tyopaikat().iterator().next()).nimi())
        .isEqualTo(ls(Kieli.EN, "VALID"));
  }
}
