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
import static org.junit.jupiter.api.Assertions.*;

import fi.okm.jod.yksilo.dto.profiili.KategoriaDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.service.profiili.KoulutusService;
import fi.okm.jod.yksilo.service.profiili.YksilonOsaaminenService;
import java.net.URI;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql("/data/osaaminen.sql")
@Import({KoulutusService.class, YksilonOsaaminenService.class})
class KoulutusServiceTest extends AbstractServiceTest {

  @Autowired KoulutusService service;

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
