/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.profiili.KategoriaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.util.Pair;
import org.springframework.test.context.jdbc.Sql;

@Sql("/data/osaaminen.sql")
@Import({KoulutusService.class, YksilonOsaaminenService.class})
class KoulutusServiceTest extends AbstractServiceTest {

  private static final UUID TEST_KOULUTUS_1_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TEST_KATEGORIA_1_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TEST_USER_1_UUID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Autowired KoulutusService service;

  @Test
  void shouldAddKoulutusWithKategoria() {
    assertDoesNotThrow(
        () -> {
          var before = service.findAll(user);
          var resultDto =
              service.create(
                  user,
                  new KategoriaDto(null, ls("kategoria"), null),
                  Set.of(
                      new KoulutusDto(
                          null,
                          ls("nimi"),
                          null,
                          null,
                          null,
                          Set.of(URI.create("urn:osaaminen1")))));
          entityManager.flush();

          assertNotNull(resultDto.kategoria());
          var after = service.findAll(user);
          assertTrue(before.size() < after.size());
          assertTrue(
              after.stream()
                  .allMatch(
                      p ->
                          p.kategoria().id() != null
                              && !p.koulutukset().isEmpty()
                              && p.koulutukset().stream().allMatch(k -> k.id() != null)));
        });
  }

  @Sql(
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
      scripts = "/data/create/koulutus-test-data.sql")
  @Sql(
      executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
      scripts = "/data/delete/koulutus-test-data.sql")
  @Test
  void shouldUpdateKoulutusWithoutKategoria() {
    assertDoesNotThrow(
        () -> {
          var user = TestJodUser.of(TEST_USER_1_UUID.toString());
          var oldKoulutus =
              service.find(user, TEST_KOULUTUS_1_UUID).koulutukset().stream()
                  .filter(k -> k.id().equals(TEST_KOULUTUS_1_UUID))
                  .findFirst()
                  .get();
          var newOsaamiset =
              Set.of(
                  URI.create("urn:osaaminen1"),
                  URI.create("urn:osaaminen2"),
                  URI.create("urn:osaaminen7"));

          var koulutusToPatch =
              new KoulutusDto(
                  TEST_KOULUTUS_1_UUID,
                  ls(
                      Pair.of(Kieli.FI, "uusi nimi"),
                      Pair.of(Kieli.EN, "new name"),
                      Pair.of(Kieli.SV, "nya namn")),
                  ls(
                      Pair.of(Kieli.FI, "uusi kuvaus"),
                      Pair.of(Kieli.EN, "new description"),
                      Pair.of(Kieli.SV, "nya beskrivning")),
                  oldKoulutus.alkuPvm(),
                  oldKoulutus.loppuPvm(),
                  newOsaamiset);
          var resultDto = service.update(user, koulutusToPatch);
          entityManager.flush();

          assertNotNull(resultDto.koulutukset());
          assertNull(resultDto.kategoria());

          var updatedKoulutus =
              service.find(user, TEST_KOULUTUS_1_UUID).koulutukset().stream()
                  .filter(k -> k.id().equals(TEST_KOULUTUS_1_UUID))
                  .findFirst()
                  .get();
          assertNotNull(updatedKoulutus);

          assertEquals("uusi nimi", updatedKoulutus.nimi().get(Kieli.FI));
          assertEquals("new name", updatedKoulutus.nimi().get(Kieli.EN));
          assertEquals("nya namn", updatedKoulutus.nimi().get(Kieli.SV));

          assertEquals("uusi kuvaus", updatedKoulutus.kuvaus().get(Kieli.FI));
          assertEquals("new description", updatedKoulutus.kuvaus().get(Kieli.EN));
          assertEquals("nya beskrivning", updatedKoulutus.kuvaus().get(Kieli.SV));

          assertEquals(newOsaamiset, updatedKoulutus.osaamiset());
        });
  }

  @Sql(
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
      scripts = "/data/create/kategoria-test-data.sql")
  @Sql(
      executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
      scripts = "/data/delete/kategoria-test-data.sql")
  @Test
  void shouldUpdateKategoria() {
    assertDoesNotThrow(
        () -> {
          var user = TestJodUser.of(TEST_USER_1_UUID.toString());
          var kategoriaToPatch =
              new KategoriaDto(
                  TEST_KATEGORIA_1_UUID,
                  ls(
                      Pair.of(Kieli.FI, "uusi nimi"),
                      Pair.of(Kieli.EN, "new name"),
                      Pair.of(Kieli.SV, "nya namn")),
                  ls(
                      Pair.of(Kieli.FI, "uusi kuvaus"),
                      Pair.of(Kieli.EN, "new description"),
                      Pair.of(Kieli.SV, "nya beskrivning")));
          var resultDto = service.update(user, kategoriaToPatch);
          entityManager.flush();

          assertNull(resultDto.koulutukset());
          assertNotNull(resultDto.kategoria());

          var updatedKategoria = service.findKategoriaById(user, TEST_KATEGORIA_1_UUID).kategoria();
          assertNotNull(updatedKategoria);

          assertEquals("uusi nimi", updatedKategoria.nimi().get(Kieli.FI));
          assertEquals("new name", updatedKategoria.nimi().get(Kieli.EN));
          assertEquals("nya namn", updatedKategoria.nimi().get(Kieli.SV));

          assertEquals("uusi kuvaus", updatedKategoria.kuvaus().get(Kieli.FI));
          assertEquals("new description", updatedKategoria.kuvaus().get(Kieli.EN));
          assertEquals("nya beskrivning", updatedKategoria.kuvaus().get(Kieli.SV));
        });
  }

  @Test
  void shouldAddAndUpdateKoulutus() {
    assertDoesNotThrow(
        () -> {
          var id =
              service.merge(
                  user,
                  new KategoriaDto(null, ls("kategoria"), null),
                  Set.of(
                      new KoulutusDto(
                          null,
                          ls("nimi"),
                          null,
                          null,
                          null,
                          Set.of(URI.create("urn:osaaminen1")))));
          entityManager.flush();

          service.upsert(
              user,
              new KategoriaDto(id.kategoria(), null, null),
              Set.of(
                  new KoulutusDto(
                      null, ls("nimi2"), null, null, null, Set.of(URI.create("urn:osaaminen1")))));
          entityManager.flush();
          entityManager.clear();

          var result = service.findAll(user);
          assertEquals(2, result.getFirst().koulutukset().size());
          assertEquals(ls("kategoria"), result.getFirst().kategoria().nimi());

          id =
              service.merge(
                  user,
                  result.getFirst().kategoria(),
                  Set.of(
                      new KoulutusDto(
                          null,
                          ls("nimi"),
                          null,
                          null,
                          null,
                          Set.of(URI.create("urn:osaaminen1")))));
          entityManager.flush();
          entityManager.clear();

          result = service.findAll(user, id.kategoria());
          assertEquals(1, result.getFirst().koulutukset().size());
        });
  }

  @Test
  void shouldHandleNullKategoriaSpecially() {
    assertDoesNotThrow(
        () -> {
          service.merge(
              user,
              null,
              Set.of(
                  new KoulutusDto(
                      null, ls("nimi"), null, null, null, Set.of(URI.create("urn:osaaminen1")))));
          entityManager.flush();

          service.merge(
              user,
              null,
              Set.of(
                  new KoulutusDto(
                      null, ls("nimi2"), null, null, null, Set.of(URI.create("urn:osaaminen1")))));
          entityManager.flush();
          entityManager.clear();

          var result = service.findAll(user);
          assertEquals(1, result.size());
          assertEquals(2, result.getFirst().koulutukset().size());
        });
  }

  @Test
  void shouldNotAllowEmptyKategoria() {
    final var kategoriaDto = new KategoriaDto(null, ls("nimi"), null);

    assertThrows(
        ServiceValidationException.class, () -> service.merge(user, kategoriaDto, Set.of()));

    assertThrows(ServiceValidationException.class, () -> service.merge(user, kategoriaDto, null));
  }
}
