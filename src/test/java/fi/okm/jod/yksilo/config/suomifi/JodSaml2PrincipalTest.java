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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JodSaml2PrincipalTest {

  @Test
  void testGetPersonId() {
    var finAttributeValue = "hetu";
    var eidasAttributeValue = "eidas-id";

    assertEquals(
        finAttributeValue,
        createJodSaml2User(
                Map.of(
                    PersonIdentifier.FIN.getAttribute().getUri(),
                    List.of(finAttributeValue),
                    PersonIdentifier.EIDAS.getAttribute().getUri(),
                    List.of(eidasAttributeValue)))
            .getPersonId());

    assertEquals(
        finAttributeValue,
        createJodSaml2User(
                Map.of(
                    PersonIdentifier.EIDAS.getAttribute().getUri(),
                    List.of(eidasAttributeValue),
                    PersonIdentifier.FIN.getAttribute().getUri(),
                    List.of(finAttributeValue)))
            .getPersonId());

    assertEquals(
        finAttributeValue,
        createJodSaml2User(
                Map.of(PersonIdentifier.FIN.getAttribute().getUri(), List.of(finAttributeValue)))
            .getPersonId());

    assertEquals(
        eidasAttributeValue,
        createJodSaml2User(
                Map.of(
                    PersonIdentifier.EIDAS.getAttribute().getUri(), List.of(eidasAttributeValue)))
            .getPersonId());
  }

  private static JodSaml2Principal createJodSaml2User(Map<String, List<Object>> attributes) {
    var name = "John Doe";
    var sessionIndexes = List.of("index1", "index2");
    var relyingPartyRegistrationId = "relyingParty123";
    var id = UUID.randomUUID();

    return new JodSaml2Principal(name, attributes, sessionIndexes, relyingPartyRegistrationId, id);
  }
}
