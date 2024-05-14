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

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.LocalizedString;
import fi.okm.jod.yksilo.dto.ToimenkuvaDto;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.service.profiili.ToimenkuvaService;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql("/data/osaaminen.sql")
@Import(ToimenkuvaService.class)
class ToimenkuvaServiceTest extends AbstractServiceTest {

  @Autowired ToimenkuvaService service;
  private UUID tyopaikka;

  @BeforeEach
  public void setUp() {
    var tyopaikka =
        new Tyopaikka(
            entityManager.find(Yksilo.class, user.getId()),
            new LocalizedString(Map.of(Kieli.FI, "Testi")));
    this.tyopaikka = entityManager.persist(tyopaikka).getId();
    entityManager.flush();
  }

  @Test
  void shouldAddToimenkuva() {
    assertDoesNotThrow(
        () -> {
          service.add(
              user,
              tyopaikka,
              new ToimenkuvaDto(
                  null,
                  ls(Kieli.FI, "nimi", Kieli.SV, "namn"),
                  null,
                  LocalDate.of(2021, 1, 1),
                  LocalDate.of(2021, 12, 31)));
          entityManager.flush();
          entityManager.clear();

          var result = service.findAll(user, tyopaikka);
          assertEquals(1, result.size());
        });
  }
}
