/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import static org.assertj.core.api.Assertions.assertThat;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.repository.AmmattiRepository;
import fi.okm.jod.yksilo.repository.OsaaminenRepository;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the ESCO import procedures. Staging rows have the shape produced by
 * scripts/esco-to-jsonl.sh: an ESCO concept with its description merged in.
 */
class EscoImportTest extends AbstractServiceTest {

  @Autowired private AmmattiRepository ammattiRepository;
  @Autowired private OsaaminenRepository osaaminenRepository;

  @Nested
  class Occupations {

    @Test
    void importAddsOccupationWithNamesAndDescriptionsInSupportedLanguages() {
      var previous = ammattiRepository.refreshAll(null);

      stage(
          "esco_data.occupations",
          """
          {"uri": "urn:esco:occupation:new", "notation": "1234.5",
           "prefLabel": {"fi": "Uusi ammatti", "sv": "Nytt yrke", "en": "New occupation", "de": "Neuer Beruf"},
           "description": {"fi": "Kuvaus", "sv": "Beskrivning", "en": "Description", "de": "Beschreibung"}}
          """);
      importData("esco_data.import_ammatti");

      var ammatti =
          ammattiRepository
              .refreshAll(previous)
              .payload()
              .get(URI.create("urn:esco:occupation:new"));
      assertThat(ammatti.koodi()).isEqualTo("1234.5");
      assertThat(ammatti.nimi().asMap())
          .isEqualTo(
              Map.of(Kieli.FI, "Uusi ammatti", Kieli.SV, "Nytt yrke", Kieli.EN, "New occupation"));
      assertThat(ammatti.kuvaus().asMap())
          .isEqualTo(Map.of(Kieli.FI, "Kuvaus", Kieli.SV, "Beskrivning", Kieli.EN, "Description"));
    }

    @Test
    void importUpdatesExistingOccupation() {
      var previous = ammattiRepository.refreshAll(null);

      stage(
          "esco_data.occupations",
          """
          {"uri": "urn:ammatti:1", "notation": "1",
           "prefLabel": {"fi": "Päivitetty nimi"},
           "description": {"fi": "Päivitetty kuvaus"}}
          """);
      importData("esco_data.import_ammatti");

      var ammatti =
          ammattiRepository.refreshAll(previous).payload().get(URI.create("urn:ammatti:1"));
      assertThat(ammatti.nimi().get(Kieli.FI)).isEqualTo("Päivitetty nimi");
      assertThat(ammatti.kuvaus().get(Kieli.FI)).isEqualTo("Päivitetty kuvaus");
    }

    @Test
    void importSkipsOccupationWithoutNotation() {
      stage(
          "esco_data.occupations",
          """
          {"uri": "urn:esco:occupation:no-notation",
           "prefLabel": {"fi": "Ei koodia"}, "description": {"fi": "Kuvaus"}}
          """);
      importData("esco_data.import_ammatti");

      assertThat(ammattiRepository.refreshAll(null).payload())
          .doesNotContainKey(URI.create("urn:esco:occupation:no-notation"));
    }
  }

  @Nested
  class Skills {

    @Test
    void importAddsSkillWithNamesAndDescriptionsInSupportedLanguages() {
      var previous = osaaminenRepository.refreshAll(null);

      stage(
          "esco_data.skills",
          """
          {"uri": "urn:esco:skill:new",
           "prefLabel": {"fi": "Uusi osaaminen", "sv": "Ny färdighet", "en": "New skill", "de": "Neue Fähigkeit"},
           "description": {"fi": "Kuvaus", "sv": "Beskrivning", "en": "Description", "de": "Beschreibung"}}
          """);
      importData("esco_data.import_osaaminen");

      var osaaminen =
          osaaminenRepository.refreshAll(previous).payload().get(URI.create("urn:esco:skill:new"));
      assertThat(osaaminen.nimi().asMap())
          .isEqualTo(
              Map.of(Kieli.FI, "Uusi osaaminen", Kieli.SV, "Ny färdighet", Kieli.EN, "New skill"));
      assertThat(osaaminen.kuvaus().asMap())
          .isEqualTo(Map.of(Kieli.FI, "Kuvaus", Kieli.SV, "Beskrivning", Kieli.EN, "Description"));
    }

    @Test
    void importUpdatesExistingSkill() {
      var previous = osaaminenRepository.refreshAll(null);

      stage(
          "esco_data.skills",
          """
          {"uri": "urn:osaaminen:1",
           "prefLabel": {"fi": "Päivitetty nimi"},
           "description": {"fi": "Päivitetty kuvaus"}}
          """);
      importData("esco_data.import_osaaminen");

      var osaaminen =
          osaaminenRepository.refreshAll(previous).payload().get(URI.create("urn:osaaminen:1"));
      assertThat(osaaminen.nimi().get(Kieli.FI)).isEqualTo("Päivitetty nimi");
      assertThat(osaaminen.kuvaus().get(Kieli.FI)).isEqualTo("Päivitetty kuvaus");
    }
  }

  private void stage(String table, String json) {
    entityManager
        .getEntityManager()
        .createNativeQuery("INSERT INTO " + table + "(data) VALUES (CAST(:data AS jsonb))")
        .setParameter("data", json)
        .executeUpdate();
  }

  private void importData(String procedure) {
    entityManager.getEntityManager().createStoredProcedureQuery(procedure).execute();
    entityManager.clear();
  }
}
