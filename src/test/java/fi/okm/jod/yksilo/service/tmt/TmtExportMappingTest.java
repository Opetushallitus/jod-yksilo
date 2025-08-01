/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.tmt;

import static fi.okm.jod.yksilo.testutil.LocalizedStrings.ls;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import fi.okm.jod.yksilo.domain.MuuOsaaminen;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Patevyys;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.external.tmt.model.FullProfileDtoExternal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;

class TmtExportMappingTest {

  @Test
  void testMapping() {
    // Create Yksilo with random data
    Yksilo yksilo = createYksilo();

    FullProfileDtoExternal profile = TmtExportService.toTmtProfile(yksilo);

    assertNotNull(profile.getEducations());
    assertEquals(
        count(yksilo.getKoulutusKokonaisuudet(), k -> k.getKoulutukset().size()),
        profile.getEducations().size());

    assertNotNull(profile.getEmployments());
    assertEquals(
        count(yksilo.getTyopaikat(), t -> t.getToimenkuvat().size()),
        profile.getEmployments().size());

    assertNotNull(profile.getProjects());
    assertEquals(
        count(yksilo.getToiminnot(), t -> t.getPatevyydet().size()), profile.getProjects().size());
  }

  @Test
  void testMappingWithEmptyData() {
    Yksilo yksilo = new Yksilo(UUID.randomUUID());
    var result = assertDoesNotThrow(() -> TmtExportService.toTmtProfile(yksilo));
    assertNotNull(result);
  }

  private static <T> int count(Collection<T> collection, ToIntFunction<T> weight) {
    return collection.stream().mapToInt(weight).sum();
  }

  private static Yksilo createYksilo() {
    var yksilo = new Yksilo(UUID.randomUUID());
    yksilo.setTervetuloapolku(true);
    yksilo
        .getOsaamiset()
        .add(
            new YksilonOsaaminen(
                new MuuOsaaminen(yksilo, Set.of()),
                new Osaaminen(URI.create("urn:osaaminen:12345"))));

    var toiminto = new Toiminto(yksilo, ls("Toiminto 1"));
    var patevyys = new Patevyys(toiminto);
    patevyys.setAlkuPvm(LocalDate.now());
    patevyys.setLoppuPvm(LocalDate.now().plusYears(1));
    patevyys.setNimi(ls("Patevyys 1"));
    patevyys.setKuvaus(ls("Patevyys Kuvaus 1"));
    patevyys
        .getOsaamiset()
        .add(new YksilonOsaaminen(patevyys, new Osaaminen(URI.create("urn:osaaminen:12345"))));
    yksilo.getOsaamiset().addAll(patevyys.getOsaamiset());
    toiminto.getPatevyydet().add(patevyys);
    yksilo.getToiminnot().add(toiminto);

    var tyopaikka = new Tyopaikka(yksilo, ls("Tyopaikka 1"));
    var toimenkuva = new Toimenkuva(tyopaikka);
    toimenkuva.setAlkuPvm(LocalDate.now());
    toimenkuva.setLoppuPvm(LocalDate.now().plusYears(1));
    toimenkuva.setNimi(ls("Toimenkuva 1"));
    toimenkuva.setKuvaus(ls("Toimenkuva Kuvaus 1"));
    toimenkuva
        .getOsaamiset()
        .add(new YksilonOsaaminen(toimenkuva, new Osaaminen(URI.create("urn:osaaminen:12345"))));
    yksilo.getOsaamiset().addAll(toimenkuva.getOsaamiset());
    tyopaikka.getToimenkuvat().add(toimenkuva);
    yksilo.getTyopaikat().add(tyopaikka);

    var koulutusKokonaisuus = new KoulutusKokonaisuus(yksilo, ls("KoulutusKokonaisuus 1"));
    var koulutus = new Koulutus(koulutusKokonaisuus);
    koulutus.setAlkuPvm(LocalDate.now());
    koulutus.setLoppuPvm(LocalDate.now().plusYears(1));
    koulutus.setNimi(ls("Koulutus 1"));
    koulutus.setKuvaus(ls("Koulutus Kuvaus 1"));
    koulutus
        .getOsaamiset()
        .add(new YksilonOsaaminen(koulutus, new Osaaminen(URI.create("urn:osaaminen:12345"))));
    yksilo.getOsaamiset().addAll(koulutus.getOsaamiset());
    koulutusKokonaisuus.getKoulutukset().add(koulutus);
    yksilo.getKoulutusKokonaisuudet().add(koulutusKokonaisuus);

    return yksilo;
  }
}
