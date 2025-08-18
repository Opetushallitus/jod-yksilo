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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import fi.okm.jod.yksilo.IntegrationTest;
import fi.okm.jod.yksilo.domain.FinnishPersonIdentifier;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.PersonIdentifierType;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.test.context.TestPropertySource;

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

  @Test
  void shouldCreateUser() {
    var nationalId = FinnishPersonIdentifier.of("010199-9997");
    var id =
        converter.upsertUser(new MockPrincipal(nationalId), Kieli.SV, PersonIdentifierType.FIN);
    var yksilo = yksilot.findById(id).orElseThrow();

    assertFalse(yksilo.getTervetuloapolku());
    assertNull(yksilo.getSukupuoli());
    assertNull(yksilo.getSyntymavuosi());
    assertNull(yksilo.getKotikunta());
  }

  @Test
  void shouldUpdateUser() {
    var nationalId = FinnishPersonIdentifier.of("010199X9986");
    var id =
        yksilot.findIdByHenkiloId(
            PersonIdentifierType.FIN.asQualifiedIdentifier(nationalId.asString()));

    {
      var yksilo = new Yksilo(id);
      yksilo.setSukupuoli(nationalId.getGender());
      yksilo.setSyntymavuosi(nationalId.getBirthYear());
      yksilo.setKotikunta("200");
      yksilot.save(yksilo);
    }

    var updated =
        converter.upsertUser(new MockPrincipal(nationalId), Kieli.SV, PersonIdentifierType.FIN);

    assertEquals(id, updated);
    var yksilo = yksilot.findById(id).orElseThrow();

    assertEquals(nationalId.getGender(), yksilo.getSukupuoli());
    assertEquals(nationalId.getBirthYear(), yksilo.getSyntymavuosi());
    assertEquals("508", yksilo.getKotikunta());
  }

  @Test
  void shouldSkipOptedOutAttributesWhenUpdatingExistingUser() {
    var nationalId = FinnishPersonIdentifier.of("010199X9986");
    var id =
        yksilot.findIdByHenkiloId(
            PersonIdentifierType.FIN.asQualifiedIdentifier(nationalId.asString()));

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

    var updated = converter.upsertUser(principal, Kieli.SV, PersonIdentifierType.FIN);

    assertEquals(id, updated);
    var yksilo = yksilot.findById(id).orElseThrow();

    assertNull(yksilo.getSukupuoli());
    assertEquals(nationalId.getBirthYear(), yksilo.getSyntymavuosi());
    assertEquals(kotikunta, yksilo.getKotikunta());
  }

  @Test
  void shouldFailIfIdentifierMissing() {
    assertThrows(
        BadCredentialsException.class,
        () -> converter.upsertUser(() -> "invalid", Kieli.EN, PersonIdentifierType.EIDAS));
  }

  @Test
  void shouldCreateEidasUser() {
    var eidasId = "FI/DE/ABC123";
    var id =
        converter.upsertUser(new MockEidasPrincipal(eidasId), Kieli.SV, PersonIdentifierType.EIDAS);
    var yksilo = yksilot.findById(id).orElseThrow();

    assertFalse(yksilo.getTervetuloapolku());
    assertNull(yksilo.getSyntymavuosi());
    assertNull(yksilo.getValittuKieli());
  }

  @Test
  void shouldUpdateEidasUserWhenSavingDemographicDataIsAllowed() {
    var eidasId = "FI/DE/XYZ789";
    var id = yksilot.findIdByHenkiloId(PersonIdentifierType.EIDAS.asQualifiedIdentifier(eidasId));
    {
      var yksilo = new Yksilo(id);
      yksilo.setSyntymavuosi(1900);
      yksilo.setValittuKieli(Kieli.EN);
      yksilot.save(yksilo);
    }

    var updated =
        converter.upsertUser(new MockEidasPrincipal(eidasId), Kieli.SV, PersonIdentifierType.EIDAS);

    assertEquals(id, updated);
    var yksilo = yksilot.findById(id).orElseThrow();

    assertEquals(1999, yksilo.getSyntymavuosi());
    assertEquals(Kieli.SV, yksilo.getValittuKieli());
  }

  record MockPrincipal(FinnishPersonIdentifier identifier) implements Saml2AuthenticatedPrincipal {
    @Override
    public String getName() {
      return identifier.asString();
    }

    @Override
    public Map<String, List<Object>> getAttributes() {
      return Map.of(
          Attribute.NATIONAL_IDENTIFICATION_NUMBER.getUri(),
          List.of(identifier.asString()),
          Attribute.KOTIKUNTA_KUNTANUMERO.getUri(),
          List.of("508"));
    }
  }

  record MockEidasPrincipal(String identifier) implements Saml2AuthenticatedPrincipal {
    @Override
    public String getName() {
      return identifier;
    }

    @Override
    public Map<String, List<Object>> getAttributes() {
      return Map.of(
          Attribute.PERSON_IDENTIFIER.getUri(),
          List.of(identifier),
          Attribute.DATE_OF_BIRTH.getUri(),
          List.of("1999-01-01"));
    }
  }
}
