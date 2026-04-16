/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.suomifi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import fi.okm.jod.yksilo.IntegrationTest;
import fi.okm.jod.yksilo.domain.FinnishPersonIdentifier;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.PersonIdentifierType;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.onr.OppijanumeroService;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(
    properties = {
      "jod.authentication.provider=suomifi",
      "spring.security.saml2.relyingparty.registration.jodsuomifi.assertingparty.entity-id=https://example.org",
      "spring.security.saml2.relyingparty.registration.jodsuomifi.assertingparty.singlesignon.url=https://example.org/sso",
      "spring.security.saml2.relyingparty.registration.jodsuomifi.assertingparty.singlesignon.sign-request=false",
    })
class ResponseTokenConverterTest extends IntegrationTest {
  @Autowired private ResponseTokenConverter converter;
  @Autowired private YksiloRepository yksilot;
  @Autowired private DataSource dataSource;
  @MockitoBean private OppijanumeroService oppijanumeroService;

  @Test
  void shouldCreateUser() {
    var nationalId = FinnishPersonIdentifier.of("010199-9997");
    when(oppijanumeroService.fetchOppijanumero(
            nationalId.asString(), "Matti Kalevi", "Matti", "Meikäläinen"))
        .thenReturn("ONR:1.2.246.562.24.00000000001");

    var jodUser =
        converter.upsertUser(new MockPrincipal(nationalId), Kieli.SV, PersonIdentifierType.FIN);
    var yksilo = yksilot.findById(jodUser.getId()).orElseThrow();

    assertFalse(yksilo.getTervetuloapolku());
    assertNull(yksilo.getSukupuoli());
    assertNull(yksilo.getSyntymavuosi());
    assertNull(yksilo.getKotikunta());
    assertName(
        PersonIdentifierType.FIN.asQualifiedIdentifier(nationalId.asString()),
        "Matti",
        "Meikäläinen");
  }

  @Test
  void shouldUpdateUser() {
    var nationalId = FinnishPersonIdentifier.of("010199X9986");
    when(oppijanumeroService.fetchOppijanumero(
            nationalId.asString(), "Matti Kalevi", "Matti", "Meikäläinen"))
        .thenReturn("ONR:1.2.246.562.24.00000000002");

    var id =
        yksilot.upsertTunnistusData(
            PersonIdentifierType.FIN.asQualifiedIdentifier(nationalId.asString()),
            "ONR:1.2.246.562.24.00000000002",
            null,
            null);

    {
      var yksilo = new Yksilo(id);
      yksilo.setSukupuoli(nationalId.getGender());
      yksilo.setSyntymavuosi(nationalId.getBirthYear());
      yksilo.setKotikunta("200");
      yksilot.save(yksilo);
    }

    var updatedJodUser =
        converter.upsertUser(new MockPrincipal(nationalId), Kieli.SV, PersonIdentifierType.FIN);

    assertEquals(id, updatedJodUser.getId());
    var yksilo = yksilot.findById(id).orElseThrow();

    assertEquals(nationalId.getGender(), yksilo.getSukupuoli());
    assertEquals(nationalId.getBirthYear(), yksilo.getSyntymavuosi());
    assertEquals("508", yksilo.getKotikunta());
    assertName(
        PersonIdentifierType.FIN.asQualifiedIdentifier(nationalId.asString()),
        "Matti",
        "Meikäläinen");
  }

  @Test
  void shouldSkipOptedOutAttributesWhenUpdatingExistingUser() {
    var nationalId = FinnishPersonIdentifier.of("010199X9975");
    when(oppijanumeroService.fetchOppijanumero(
            nationalId.asString(), "Matti Kalevi", "Matti", "Meikäläinen"))
        .thenReturn("ONR:1.2.246.562.24.00000000003");

    var id =
        yksilot.upsertTunnistusData(
            PersonIdentifierType.FIN.asQualifiedIdentifier(nationalId.asString()),
            "ONR:1.2.246.562.24.00000000003",
            null,
            null);

    var principal = new MockPrincipal(nationalId);
    var kotikunta = principal.<String>getFirstAttribute(Attribute.KOTIKUNTA_KUNTANUMERO.getUri());
    assertNotEquals("200", kotikunta);

    {
      var yksilo = new Yksilo(id);
      yksilo.setSukupuoli(null);
      yksilo.setSyntymavuosi(nationalId.getBirthYear());
      yksilo.setKotikunta("200");
      yksilot.save(yksilo);
    }

    var updatedJodUser = converter.upsertUser(principal, Kieli.SV, PersonIdentifierType.FIN);

    assertEquals(id, updatedJodUser.getId());
    var yksilo = yksilot.findById(id).orElseThrow();

    assertNull(yksilo.getSukupuoli());
    assertEquals(nationalId.getBirthYear(), yksilo.getSyntymavuosi());
    assertEquals(kotikunta, yksilo.getKotikunta());
    assertName(
        PersonIdentifierType.FIN.asQualifiedIdentifier(nationalId.asString()),
        "Matti",
        "Meikäläinen");
  }

  @Test
  void shouldBackfillOppijanumeroForExistingUserWithout() {
    var nationalId = FinnishPersonIdentifier.of("010100A998M");
    var qualifiedId = PersonIdentifierType.FIN.asQualifiedIdentifier(nationalId.asString());
    var oppijanumero = "ONR:1.2.246.562.24.00000000004";

    // Create user without oppijanumero (simulates pre-Stage1 user)
    var id = yksilot.upsertTunnistusData(qualifiedId, null, null, null);
    yksilot.save(new Yksilo(id));

    // Verify oppijanumero is initially missing
    var before = yksilot.findTunnistusDataByHenkiloId(qualifiedId).orElseThrow();
    assertNull(before.oppijanumero());

    when(oppijanumeroService.fetchOppijanumero(
            nationalId.asString(), "Matti Kalevi", "Matti", "Meikäläinen"))
        .thenReturn(oppijanumero);

    var jodUser =
        converter.upsertUser(new MockPrincipal(nationalId), Kieli.FI, PersonIdentifierType.FIN);

    assertEquals(id, jodUser.getId());

    // Verify oppijanumero was actually persisted
    var after = yksilot.findTunnistusDataByHenkiloId(qualifiedId).orElseThrow();
    assertNotNull(after.oppijanumero());

    // Verify the oppijanumero identifier also resolves to the same user via read_yksilo_name
    assertName(oppijanumero, "Matti", "Meikäläinen");
  }

  @Test
  void shouldLinkFinLoginToExistingMpassidFirstAccount() {
    var oppijanumero = "ONR:1.2.246.562.24.00000000005";

    // Simulate MPASSid-first account: oppijanumero only, no henkilo_id
    var mpassidId = yksilot.upsertTunnistusData(null, oppijanumero, null, null);
    yksilot.save(new Yksilo(mpassidId));

    // Now a FIN user logs in and ONR resolves to the same oppijanumero
    var nationalId = FinnishPersonIdentifier.of("020200A900X");
    when(oppijanumeroService.fetchOppijanumero(
            nationalId.asString(), "Matti Kalevi", "Matti", "Meikäläinen"))
        .thenReturn(oppijanumero);

    var jodUser =
        converter.upsertUser(new MockPrincipal(nationalId), Kieli.FI, PersonIdentifierType.FIN);

    // FIN login should land on the same account created by MPASSid
    assertEquals(mpassidId, jodUser.getId());

    // Both identifiers should now resolve via name lookup
    var qualifiedFin = PersonIdentifierType.FIN.asQualifiedIdentifier(nationalId.asString());
    assertName(qualifiedFin, "Matti", "Meikäläinen");
    assertName(oppijanumero, "Matti", "Meikäläinen");

    // After linking, the account should have oppijanumero set
    var linked = yksilot.findTunnistusDataByHenkiloId(qualifiedFin).orElseThrow();
    assertNotNull(linked.oppijanumero());

    // Subsequent FIN login should still resolve to the same account
    var returning =
        converter.upsertUser(new MockPrincipal(nationalId), Kieli.FI, PersonIdentifierType.FIN);
    assertEquals(mpassidId, returning.getId());
  }

  @Test
  void shouldKeepAccountsSeparateWhenOppijanumeroAlreadyInUse() {
    var oppijanumero = "ONR:1.2.246.562.24.00000000006";

    // MPASSid-first account: oppijanumero only, no henkilo_id
    var mpassidId = yksilot.upsertTunnistusData(null, oppijanumero, "Matti", "Meikäläinen");
    yksilot.save(new Yksilo(mpassidId));

    // Suomi.fi account already exists with hetu, but no oppijanumero
    var nationalId = FinnishPersonIdentifier.of("030300A900C");
    var qualifiedFin = PersonIdentifierType.FIN.asQualifiedIdentifier(nationalId.asString());
    var suomifiId = yksilot.upsertTunnistusData(qualifiedFin, null, null, null);
    yksilot.save(new Yksilo(suomifiId));

    assertNotEquals(mpassidId, suomifiId);

    // ONR returns the same oppijanumero that already belongs to the MPASSid account
    when(oppijanumeroService.fetchOppijanumero(
            nationalId.asString(), "Matti Kalevi", "Matti", "Meikäläinen"))
        .thenReturn(oppijanumero);

    // Login via Suomi.fi should succeed, keeping accounts separate
    var jodUser =
        converter.upsertUser(new MockPrincipal(nationalId), Kieli.FI, PersonIdentifierType.FIN);

    // Should land on the existing Suomi.fi account, not the MPASSid one
    assertEquals(suomifiId, jodUser.getId());

    // Oppijanumero should remain null on the Suomi.fi account
    var suomifiData = yksilot.findTunnistusDataByHenkiloId(qualifiedFin).orElseThrow();
    assertNull(suomifiData.oppijanumero());

    // The MPASSid account should be untouched
    var mpassidData = yksilot.findTunnistusDataByOppijanumero(oppijanumero).orElseThrow();
    assertEquals(mpassidId, mpassidData.yksiloId());
  }

  @Test
  void shouldFailIfIdentifierMissing() {
    assertThrows(
        BadCredentialsException.class,
        () -> converter.upsertUser(new MockPrincipal(null), Kieli.EN, PersonIdentifierType.EIDAS));
  }

  @Test
  void shouldCreateEidasUser() {
    var eidasId = "FI/DE/ABC123";
    var jodUser =
        converter.upsertUser(new MockEidasPrincipal(eidasId), Kieli.SV, PersonIdentifierType.EIDAS);
    var yksilo = yksilot.findById(jodUser.getId()).orElseThrow();

    assertFalse(yksilo.getTervetuloapolku());
    assertNull(yksilo.getSyntymavuosi());
    assertNull(yksilo.getValittuKieli());
  }

  @Test
  void shouldUpdateEidasUserWhenSavingDemographicDataIsAllowed() {
    var eidasId = "FI/DE/XYZ789";
    var id =
        yksilot.upsertTunnistusData(
            PersonIdentifierType.EIDAS.asQualifiedIdentifier(eidasId), null, null, null);
    {
      var yksilo = new Yksilo(id);
      yksilo.setSyntymavuosi(1900);
      yksilo.setValittuKieli(Kieli.EN);
      yksilot.save(yksilo);
    }

    var updatedJodUser =
        converter.upsertUser(new MockEidasPrincipal(eidasId), Kieli.SV, PersonIdentifierType.EIDAS);

    assertEquals(id, updatedJodUser.getId());
    var yksilo = yksilot.findById(id).orElseThrow();

    assertEquals(1999, yksilo.getSyntymavuosi());
    assertEquals(Kieli.SV, yksilo.getValittuKieli());
    assertName(PersonIdentifierType.EIDAS.asQualifiedIdentifier(eidasId), "John", "Doe");
  }

  private void assertName(String identifier, String expectedFirstName, String expectedLastName) {
    try (var conn = dataSource.getConnection();
        var stmt = conn.prepareStatement("SELECT * FROM tunnistus.read_yksilo_name(?)")) {
      stmt.setString(1, identifier);
      try (var rs = stmt.executeQuery()) {
        if (rs.next()) {
          var etunimi = rs.getString("etunimi");
          var sukunimi = rs.getString("sukunimi");
          assertEquals(expectedFirstName, etunimi);
          assertEquals(expectedLastName, sukunimi);
        } else {
          throw new AssertionError("No result from read_yksilo_name");
        }
      }
    } catch (SQLException e) {
      throw new AssertionError("SQL Exception during test", e);
    }
  }

  record MockPrincipal(FinnishPersonIdentifier identifier)
      implements Saml2ResponseAssertionAccessor {

    @Override
    public String getNameId() {
      return "";
    }

    @Override
    public List<String> getSessionIndexes() {
      return List.of();
    }

    @Override
    public Map<String, List<Object>> getAttributes() {
      return Map.of(
          Attribute.NATIONAL_IDENTIFICATION_NUMBER.getUri(),
          List.of(identifier == null ? List.of() : identifier.asString()),
          Attribute.KOTIKUNTA_KUNTANUMERO.getUri(),
          List.of("508"),
          Attribute.FIRST_NAME.getUri(),
          List.of("Matti Kalevi"),
          Attribute.GIVEN_NAME.getUri(),
          List.of("Matti"),
          Attribute.SN.getUri(),
          List.of("Meikäläinen"));
    }

    @Override
    public String getResponseValue() {
      return "";
    }
  }

  record MockEidasPrincipal(String identifier) implements Saml2ResponseAssertionAccessor {

    @Override
    public String getNameId() {
      return "";
    }

    @Override
    public List<String> getSessionIndexes() {
      return List.of();
    }

    @Override
    public Map<String, List<Object>> getAttributes() {
      return Map.of(
          Attribute.PERSON_IDENTIFIER.getUri(),
          List.of(identifier),
          Attribute.DATE_OF_BIRTH.getUri(),
          List.of("1999-01-01"),
          Attribute.FIRST_NAME.getUri(),
          List.of("John"),
          Attribute.FAMILY_NAME.getUri(),
          List.of("Doe"));
    }

    @Override
    public String getResponseValue() {
      return "";
    }
  }
}
