/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import fi.okm.jod.yksilo.config.feature.FeatureConfiguration;
import fi.okm.jod.yksilo.config.koski.KoskiOauth2Config;
import fi.okm.jod.yksilo.config.mapping.MappingConfig;
import fi.okm.jod.yksilo.controller.koski.TestKoskiOauth2Config;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.KoskiTehtavaTila;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaDto;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaSaveDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.errorhandler.ErrorInfoFactory;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import fi.okm.jod.yksilo.service.OsaaminenService;
import fi.okm.jod.yksilo.service.profiili.KoulutusKokonaisuusService;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.core.exception.SdkClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@TestPropertySource(
    properties = {
      "jod.koski.enabled=true",
      "jod.koski.extraction.timeout=PT15M",
      "jod.koski.extraction.max-request-bytes=" + KoskiExtractionTest.MAX_REQUEST_BYTES,
      "jod.feature.flags.KOSKI_EXTRACTION=true"
    })
@EnableConfigurationProperties(FeatureConfiguration.class)
@Import({
  ErrorInfoFactory.class,
  KoskiOauth2Config.class,
  TestKoskiOauth2Config.class,
  KoskiService.class,
  KoskiReader.class,
  KoskiExtractionHandler.class,
  LocalValidatorFactoryBean.class,
  MappingConfig.class,
  ObjectMapper.class,
  KoulutusKokonaisuusService.class,
  KoulutusService.class,
  OsaaminenService.class,
  YksilonOsaaminenService.class,
})
class KoskiExtractionTest extends AbstractServiceTest {

  static final int MAX_REQUEST_BYTES = 2000;
  private static final URI SKILL = URI.create("urn:osaaminen:1");
  private static final URI OTHER_SKILL = URI.create("urn:osaaminen:2");
  private static final URI UNKNOWN_SKILL = URI.create("urn:osaaminen:unknown");

  @Autowired private KoskiService koskiService;
  @Autowired private KoulutusKokonaisuusService koulutusKokonaisuusService;
  @Autowired private KoskiExtractionHandler handler;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private KoskiClient koskiClient;
  @MockitoBean private KoskiExtractionSender sender;

  private final OAuth2AccessToken token =
      new OAuth2AccessToken(
          OAuth2AccessToken.TokenType.BEARER,
          "token",
          Instant.now(),
          Instant.now().plusSeconds(600));

  @Nested
  class Submitting {

    @Test
    void shouldFetchFromKoski_whenImportStarted() {
      when(koskiClient.fetch(user, token)).thenReturn(koskiResponse());

      var tehtava = koskiService.startImport(user, token);

      assertThat(tehtava.tila()).isEqualTo(KoskiTehtavaTila.ODOTTAA);
      assertThat(sentRequest().taskId()).isEqualTo(tehtava.id());
    }

    @Test
    void shouldNotCallKoski_whenAnImportIsBeingProcessed() {
      var first = koskiService.submit(user, koskiResponse());

      var second = koskiService.startImport(user, token);

      assertThat(second.id()).isEqualTo(first.id());
      verifyNoInteractions(koskiClient);
      verify(sender).send(any());
    }

    @Test
    void shouldSendEducationsForExtraction_whenImportSubmitted() {
      var tehtava = koskiService.submit(user, koskiResponse());

      assertThat(tehtava.tila()).isEqualTo(KoskiTehtavaTila.ODOTTAA);
      var request = sentRequest();
      assertThat(request.taskId()).isEqualTo(tehtava.id());
      assertThat(request.userId()).isEqualTo(user.getId());
      assertThat(request.koulutuskokonaisuudet())
          .singleElement()
          .satisfies(
              k ->
                  assertThat(k.koulutukset())
                      .singleElement()
                      .satisfies(
                          koulutus -> {
                            assertThat(koulutus.nimi().get(Kieli.FI))
                                .isEqualTo("Kauppatieteiden kandidaatti");
                            assertThat(koulutus.opinnot())
                                .extracting(o -> o.nimi().get(Kieli.FI))
                                .containsExactly("Mikrotaloustiede");
                          }));
    }

    @Test
    void shouldImportWithoutSkills_whenRequestIsTooLargeForExtraction() {
      var tooLarge =
          objectMapper.readTree(
              """
                  {"opiskeluoikeudet": [{
                    "oppilaitos": {"nimi": {"fi": "Yliopisto"}},
                    "suoritukset": [{"koulutusmoduuli": {
                      "tunniste": {"nimi": {"fi": "Tutkinto"}}, "kuvaus": {"fi": "%s"}}}]
                  }]}
                  """
                  .formatted("Kuvaus ".repeat(MAX_REQUEST_BYTES / 7)));

      var tehtava = koskiService.submit(user, tooLarge);

      assertCompletedWithoutSkills(koskiService.getStatus(user, tehtava.id()));
      verifyNoInteractions(sender);
    }

    @Test
    void shouldCompleteWithoutWaiting_whenThereIsNothingToExtract() {
      var generalEducationOnly = koskiResponse("Lukion oppimäärä", "lukiokoulutus");

      var tehtava = koskiService.submit(user, generalEducationOnly);

      assertCompletedWithoutSkills(tehtava);
    }

    @Test
    void shouldReplaceProcessedButUnsavedImport_whenSubmittedAgain() {
      var first = koskiService.submit(user, koskiResponse());
      handler.handleResponse(extracted(user.getId(), SKILL));

      var second = koskiService.submit(user, koskiResponse());

      assertReplaced(first, second);
    }
  }

  @Nested
  class ReceivingExtraction {

    @Test
    void shouldCompleteTheTaskWithTheExtractedSkills() {
      var tehtava = koskiService.submit(user, koskiResponse());

      handler.handleResponse(extracted(user.getId(), SKILL, OTHER_SKILL));

      assertThat(koskiService.getStatus(user, tehtava.id()).tila())
          .isEqualTo(KoskiTehtavaTila.VALMIS);
      var koulutus = onlyEducation(tehtava.id());
      assertThat(koulutus.nimi().get(Kieli.FI)).isEqualTo("Kauppatieteiden kandidaatti");
      assertThat(koulutus.osaamiset()).containsExactly(SKILL, OTHER_SKILL);
    }

    @Test
    void shouldIgnoreSkillsMissingFromTheTaxonomy() {
      var tehtava = koskiService.submit(user, koskiResponse());

      handler.handleResponse(extracted(user.getId(), SKILL, UNKNOWN_SKILL));

      assertThat(onlyEducation(tehtava.id()).osaamiset()).containsExactly(SKILL);
    }

    @Test
    void shouldCompleteWithoutSkills_whenExtractionFails() {
      var tehtava = koskiService.submit(user, koskiResponse());

      handler.handleResponse(KoskiExtractionResponse.failed(tehtava.id(), user.getId()));

      assertCompletedWithoutSkills(koskiService.getStatus(user, tehtava.id()));
    }

    @Test
    void shouldCompleteWithoutSkills_whenExtractionFindsNoSkills() {
      var tehtava = koskiService.submit(user, koskiResponse());

      handler.handleResponse(
          new KoskiExtractionResponse(
              tehtava.id(), user.getId(), KoskiExtractionResponse.SUCCESS, Map.of()));

      assertCompletedWithoutSkills(koskiService.getStatus(user, tehtava.id()));
    }

    @Test
    void shouldDiscardTheExtraction_whenTheTaskWasDeleted() {
      var tehtava = koskiService.submit(user, koskiResponse());
      koskiService.delete(user, tehtava.id());

      handler.handleResponse(extracted(user.getId(), SKILL));

      assertThat(koskiService.getStatus(user, tehtava.id()).tila())
          .isEqualTo(KoskiTehtavaTila.POISTETTU);
    }

    @Test
    void shouldIgnoreTheExtraction_whenItIsForAnotherUser() {
      var tehtava = koskiService.submit(user, koskiResponse());

      handler.handleResponse(extracted(user2.getId(), SKILL));

      assertThat(koskiService.getStatus(user, tehtava.id()).tila())
          .isEqualTo(KoskiTehtavaTila.ODOTTAA);
    }
  }

  @Nested
  class SendingFails {

    @Test
    void shouldCompleteWithoutSkills_whenTheRequestCannotBeSent() {
      doThrow(SdkClientException.create("SQS not reachable")).when(sender).send(any());

      var tehtava = koskiService.submit(user, koskiResponse());

      assertCompletedWithoutSkills(tehtava);
      assertCompletedWithoutSkills(koskiService.getStatus(user, tehtava.id()));
    }

    /** The local sender is not a bean here; it is built around the real handler. */
    @Test
    void shouldCompleteWithoutSkills_whenTheLocalExtractionServiceFails() {
      var tehtava = koskiService.submit(user, koskiResponse());
      var restClient = RestClient.builder();
      MockRestServiceServer.bindTo(restClient)
          .build()
          .expect(_ -> {})
          .andRespond(withServerError());

      new LocalKoskiExtractionSender("http://extraction.test", handler, restClient)
          .send(sentRequest());

      assertCompletedWithoutSkills(koskiService.getStatus(user, tehtava.id()));
    }
  }

  @Nested
  class UnfinishedImports {

    @Test
    void shouldListAnUnfinishedImportUntilItIsDeleted() {
      var tehtava = koskiService.submit(user, koskiResponse());

      assertThat(koskiService.findUnfinished(user))
          .singleElement()
          .satisfies(
              found -> {
                assertThat(found.id()).isEqualTo(tehtava.id());
                assertThat(found.tila()).isEqualTo(KoskiTehtavaTila.ODOTTAA);
              });

      handler.handleResponse(extracted(user.getId(), SKILL));
      assertThat(koskiService.findUnfinished(user))
          .extracting(KoskiTehtavaDto::tila)
          .containsExactly(KoskiTehtavaTila.VALMIS);

      koskiService.delete(user, tehtava.id());
      assertThat(koskiService.findUnfinished(user)).isEmpty();
    }

    @Test
    void shouldKeepOnlyTheSelectedSkills_whenSaved() {
      var tehtava = koskiService.submit(user, koskiResponse());
      handler.handleResponse(extracted(user.getId(), SKILL, OTHER_SKILL));
      var kokonaisuus =
          koskiService.getStatus(user, tehtava.id()).tulos().koulutuskokonaisuudet().getFirst();
      var koulutusId = kokonaisuus.koulutukset().iterator().next().id();
      var selection =
          new KoskiTehtavaSaveDto.Valinta(
              kokonaisuus.id(), Set.of(new KoskiTehtavaSaveDto.Lapsi(koulutusId, Set.of(SKILL))));

      koskiService.save(user, tehtava.id(), new KoskiTehtavaSaveDto(List.of(selection)));
      simulateCommit();

      assertThat(koulutusKokonaisuusService.findAll(user))
          .singleElement()
          .satisfies(
              saved ->
                  assertThat(saved.koulutukset())
                      .singleElement()
                      .satisfies(
                          koulutus -> assertThat(koulutus.osaamiset()).containsExactly(SKILL)));
    }
  }

  @Nested
  @TestPropertySource(properties = "jod.koski.extraction.timeout=PT0S")
  class AfterTheTimeout {

    @Test
    void shouldCompleteWithoutSkills_whenTheTaskIsOpened() {
      var tehtava = koskiService.submit(user, koskiResponse());

      assertCompletedWithoutSkills(koskiService.getStatus(user, tehtava.id()));
    }

    @Test
    void shouldReplaceTheTimedOutImport_whenSubmittedAgain() {
      var first = koskiService.submit(user, koskiResponse());

      var second = koskiService.submit(user, koskiResponse());

      assertReplaced(first, second);
    }

    @Test
    void shouldKeepWaiting_whenUnfinishedImportsAreListed() {
      koskiService.submit(user, koskiResponse());

      assertThat(koskiService.findUnfinished(user))
          .extracting(KoskiTehtavaDto::tila)
          .containsExactly(KoskiTehtavaTila.ODOTTAA);
    }

    @Test
    void shouldAcceptLateExtraction_whenTheTaskWasNotOpened() {
      var tehtava = koskiService.submit(user, koskiResponse());

      handler.handleResponse(extracted(user.getId(), SKILL, OTHER_SKILL));

      assertThat(onlyEducation(tehtava.id()).osaamiset()).containsExactly(SKILL, OTHER_SKILL);
    }
  }

  @Nested
  @TestPropertySource(properties = "jod.feature.flags.KOSKI_EXTRACTION=false")
  class WithExtractionTurnedOff {

    @Test
    void shouldCompleteWithoutSkills_whenImportSubmitted() {
      var tehtava = koskiService.submit(user, koskiResponse());

      assertCompletedWithoutSkills(tehtava);
      verifyNoInteractions(sender);
    }
  }

  private JsonNode koskiResponse() {
    return koskiResponse("Kauppatieteiden kandidaatti", "kauppatieteet");
  }

  private JsonNode koskiResponse(String degree, String opiskeluoikeudenTyyppi) {
    return objectMapper.readTree(
        """
            {
              "henkilö": {"hetu": "010199-9986"},
              "opiskeluoikeudet": [{
                "oppilaitos": {"nimi": {"fi": "Yliopisto"}},
                "tyyppi": {"koodiarvo": "%s"},
                "suoritukset": [{
                  "koulutusmoduuli": {"tunniste": {"nimi": {"fi": "%s"}}},
                  "osasuoritukset": [{"koulutusmoduuli": {"tunniste": {"nimi": {"fi": "Mikrotaloustiede"}}}}]
                }]
              }],
              "tokenInfo": {"scope": "HENKILOTIEDOT_HETU OPISKELUOIKEUDET_KAIKKI_TIEDOT"}
            }
            """
            .formatted(opiskeluoikeudenTyyppi, degree));
  }

  private KoskiExtractionRequest sentRequest() {
    var captor = ArgumentCaptor.forClass(KoskiExtractionRequest.class);
    verify(sender, atLeastOnce()).send(captor.capture());
    return captor.getValue();
  }

  /** The extraction service's answer giving the education of the sent request these skills. */
  private KoskiExtractionResponse extracted(UUID userId, URI... skills) {
    var request = sentRequest();
    var koulutusId = request.koulutuskokonaisuudet().getFirst().koulutukset().getFirst().id();
    return new KoskiExtractionResponse(
        request.taskId(),
        userId,
        KoskiExtractionResponse.SUCCESS,
        Map.of(koulutusId, List.of(skills)));
  }

  private KoulutusDto onlyEducation(UUID tehtavaId) {
    return koskiService
        .getStatus(user, tehtavaId)
        .tulos()
        .koulutuskokonaisuudet()
        .getFirst()
        .koulutukset()
        .iterator()
        .next();
  }

  private void assertReplaced(KoskiTehtavaDto first, KoskiTehtavaDto second) {
    assertThat(koskiService.findUnfinished(user))
        .extracting(KoskiTehtavaDto::id)
        .containsExactly(second.id());
    assertThat(koskiService.getStatus(user, first.id()).tila())
        .isEqualTo(KoskiTehtavaTila.POISTETTU);
  }

  private static void assertCompletedWithoutSkills(KoskiTehtavaDto tehtava) {
    assertThat(tehtava.tila()).isEqualTo(KoskiTehtavaTila.VALMIS);
    assertThat(tehtava.tulos().koulutuskokonaisuudet())
        .singleElement()
        .satisfies(
            kokonaisuus ->
                assertThat(kokonaisuus.koulutukset())
                    .singleElement()
                    .satisfies(koulutus -> assertThat(koulutus.osaamiset()).isEmpty()));
  }
}
