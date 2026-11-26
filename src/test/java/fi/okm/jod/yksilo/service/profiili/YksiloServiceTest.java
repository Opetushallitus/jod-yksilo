/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fi.okm.jod.yksilo.config.login.Attribute;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.PersonIdentifierType;
import fi.okm.jod.yksilo.domain.Sukupuoli;
import fi.okm.jod.yksilo.dto.profiili.TavoiteDto;
import fi.okm.jod.yksilo.dto.profiili.YksiloDto;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import fi.okm.jod.yksilo.service.NotFoundException;
import fi.okm.jod.yksilo.service.ServiceValidationException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = {"/data/mahdollisuudet-test-data.sql"})
@Import({YksiloService.class, TavoiteService.class})
class YksiloServiceTest extends AbstractServiceTest {
  @Autowired private YksiloService service;
  @Autowired private TavoiteService tavoiteService;
  @Autowired private TyomahdollisuusRepository tyomahdollisuudet;

  @Test
  void shouldDeleteUserProfile() {
    tavoiteService.add(
        user,
        new TavoiteDto(
            null,
            MahdollisuusTyyppi.TYOMAHDOLLISUUS,
            tyomahdollisuudet.findAll().getFirst().getId(),
            ls("tavoite"),
            ls("kuvaus"),
            null,
            emptySet(),
            emptySet()));
    simulateCommit();
    assertDoesNotThrow(() -> service.delete(user));
    assertEquals(0, tavoiteService.findAll(user).size());
    assertThrows(NotFoundException.class, () -> service.get(user));
  }

  @Test
  void shouldUpdateTiedot() {
    var tiedot =
        new YksiloDto(
            null,
            true,
            true,
            true,
            1999,
            Sukupuoli.NAINEN,
            user.getAttribute(Attribute.KOTIKUNTA_KUNTANUMERO).get(),
            "fi",
            Kieli.EN,
            "user@example.org");
    service.update(user, tiedot);
    assertEquals(tiedot, service.get(user));
  }

  @Test
  void shouldNotChangeIdentificationAttributes() {
    shouldUpdateTiedot();

    var tiedot =
        new YksiloDto(
            null,
            true,
            true,
            true,
            2000,
            Sukupuoli.MIES,
            "200",
            "fi",
            Kieli.EN,
            "user@example.org");
    assertThrows(ServiceValidationException.class, () -> service.update(user, tiedot));
  }

  /**
   * An eIDAS authenticated user whose response also contains a Finnish national identifier. The
   * birth year in the national identifier (1999) differs from the eIDAS date of birth (1985) so
   * that it is observable which one is used.
   */
  @Nested
  class EidasUserWithFinnishIdentifier {

    private static final String HETU = "010199-9986";
    private static final int EIDAS_BIRTH_YEAR = 1985;

    private JodUser eidasUser;

    @BeforeEach
    void createEidasUser() {
      var eidasId = "FI/FI/" + HETU;
      var yksilo =
          new Yksilo(
              yksiloRepository.upsertTunnistusData(
                  PersonIdentifierType.EIDAS.asQualifiedIdentifier(eidasId), null, "Eva", "Eidas"));
      eidasUser = new EidasUser(entityManager.persist(yksilo).getId(), eidasId, HETU);
    }

    @Test
    void isReportedAsEidasWithBirthYearFromDateOfBirth() {
      var tiedot = service.get(eidasUser);

      assertEquals(PersonIdentifierType.EIDAS, tiedot.tunnisteTyyppi());
      assertEquals(EIDAS_BIRTH_YEAR, tiedot.syntymavuosi());
    }

    @Test
    void canConfirmBirthYearFromDateOfBirth() {
      var tiedot =
          new YksiloDto(null, true, true, true, EIDAS_BIRTH_YEAR, null, null, "fi", Kieli.EN, null);

      assertDoesNotThrow(() -> service.update(eidasUser, tiedot));
    }

    private record EidasUser(UUID id, String eidasId, String hetu) implements JodUser {

      @Override
      public UUID getId() {
        return id;
      }

      @Override
      public String givenName() {
        return "Eva";
      }

      @Override
      public String familyName() {
        return "Eidas";
      }

      @Override
      public PersonIdentifierType personIdentifierType() {
        return PersonIdentifierType.EIDAS;
      }

      @Override
      public String getPersonId() {
        return eidasId;
      }

      @Override
      public String getQualifiedPersonId() {
        return PersonIdentifierType.EIDAS.asQualifiedIdentifier(eidasId);
      }

      @Override
      public Optional<String> getAttribute(Attribute attribute) {
        return switch (attribute) {
          case PERSON_IDENTIFIER -> Optional.of(eidasId);
          case NATIONAL_IDENTIFICATION_NUMBER -> Optional.of(hetu);
          case DATE_OF_BIRTH -> Optional.of(EIDAS_BIRTH_YEAR + "-06-15");
          default -> Optional.empty();
        };
      }
    }
  }
}
