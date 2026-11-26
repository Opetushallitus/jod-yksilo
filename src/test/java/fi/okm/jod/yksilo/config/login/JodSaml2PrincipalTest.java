/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.login;

import static org.assertj.core.api.Assertions.assertThat;

import fi.okm.jod.yksilo.config.SessionConfig;
import fi.okm.jod.yksilo.domain.PersonIdentifierType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

class JodSaml2PrincipalTest {

  private static final String HETU = "010199-9986";
  private static final String EIDAS_ID = "FI/FI/" + HETU;

  /**
   * Attributes returned by suomi.fi for a user having both identifiers (mutable collections, like
   * the ones produced by the SAML response parsing).
   */
  private static final Map<String, List<Object>> BOTH_IDENTIFIERS =
      new LinkedHashMap<>(
          Map.of(
              Attribute.NATIONAL_IDENTIFICATION_NUMBER.getUri(), new ArrayList<>(List.of(HETU)),
              Attribute.PERSON_IDENTIFIER.getUri(), new ArrayList<>(List.of(EIDAS_ID))));

  @Nested
  class PersonIdentifier {

    @Test
    void isNationalIdentifierWhenAuthenticatedWithFinnishMethod() {
      var principal =
          new JodSaml2Principal(UUID.randomUUID(), PersonIdentifierType.FIN, BOTH_IDENTIFIERS);

      assertThat(principal.getPersonId()).isEqualTo(HETU);
      assertThat(principal.getQualifiedPersonId()).isEqualTo("FIN:" + HETU);
      assertThat(principal.getName()).isEqualTo("FIN:" + HETU);
    }

    @Test
    void isEidasIdentifierWhenAuthenticatedWithEidasEvenIfNationalIdentifierIsPresent() {
      var principal =
          new JodSaml2Principal(UUID.randomUUID(), PersonIdentifierType.EIDAS, BOTH_IDENTIFIERS);

      assertThat(principal.getPersonId()).isEqualTo(EIDAS_ID);
      assertThat(principal.getQualifiedPersonId()).isEqualTo("EIDAS:" + EIDAS_ID);
      assertThat(principal.getName()).isEqualTo("EIDAS:" + EIDAS_ID);
    }
  }

  @Nested
  class SessionSerialization {

    private final RedisSerializer<Object> serializer = createSerializer();

    @Test
    void preservesIdentifierType() {
      var id = UUID.randomUUID();
      var principal = new JodSaml2Principal(id, PersonIdentifierType.EIDAS, BOTH_IDENTIFIERS);

      var deserialized = serializer.deserialize(serializer.serialize(principal));

      assertThat(deserialized)
          .isInstanceOfSatisfying(
              JodSaml2Principal.class,
              it -> {
                assertThat(it.getId()).isEqualTo(id);
                assertThat(it.getQualifiedPersonId()).isEqualTo("EIDAS:" + EIDAS_ID);
              });
    }

    /** Sessions serialized before the identifier type was stored in the principal. */
    @Nested
    class StoredWithoutIdentifierType {

      @Test
      void isFinnishWhenOnlyNationalIdentifierIsPresent() {
        var deserialized =
            deserializeLegacySession(
                Map.of(Attribute.NATIONAL_IDENTIFICATION_NUMBER.getUri(), HETU));

        assertThat(deserialized.getQualifiedPersonId()).isEqualTo("FIN:" + HETU);
      }

      @Test
      void isEidasWhenEidasIdentifierIsPresentEvenIfNationalIdentifierIsPresent() {
        var deserialized =
            deserializeLegacySession(
                Map.of(
                    Attribute.NATIONAL_IDENTIFICATION_NUMBER.getUri(), HETU,
                    Attribute.PERSON_IDENTIFIER.getUri(), EIDAS_ID));

        assertThat(deserialized.getQualifiedPersonId()).isEqualTo("EIDAS:" + EIDAS_ID);
      }

      private JodSaml2Principal deserializeLegacySession(Map<String, String> attributes) {
        var attributesJson =
            attributes.entrySet().stream()
                .map(
                    e ->
                        "\"%s\":[\"java.util.ArrayList\",[\"%s\"]]"
                            .formatted(e.getKey(), e.getValue()))
                .collect(Collectors.joining(","));
        var legacySession =
            """
            {"@class":"%s","id":"%s","attributes":{"@class":"java.util.HashMap",%s}}"""
                .formatted(JodSaml2Principal.class.getName(), UUID.randomUUID(), attributesJson);

        return (JodSaml2Principal)
            serializer.deserialize(legacySession.getBytes(StandardCharsets.UTF_8));
      }
    }

    private static RedisSerializer<Object> createSerializer() {
      var config = new SessionConfig();
      config.setBeanClassLoader(JodSaml2PrincipalTest.class.getClassLoader());
      return config.springSessionDefaultRedisSerializer();
    }
  }
}
