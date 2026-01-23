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
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.MuuOsaaminen;
import fi.okm.jod.yksilo.domain.SuosikkiTyyppi;
import fi.okm.jod.yksilo.dto.profiili.JakolinkkiContentDto;
import fi.okm.jod.yksilo.dto.profiili.JakolinkkiUpdateDto;
import fi.okm.jod.yksilo.dto.profiili.TavoiteDto;
import fi.okm.jod.yksilo.entity.Ammatti;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.Osaaminen;
import fi.okm.jod.yksilo.entity.Tavoite;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonOsaaminen;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import fi.okm.jod.yksilo.repository.KoulutusKokonaisuusRepository;
import fi.okm.jod.yksilo.repository.KoulutusmahdollisuusRepository;
import fi.okm.jod.yksilo.repository.TavoiteRepository;
import fi.okm.jod.yksilo.repository.ToimintoRepository;
import fi.okm.jod.yksilo.repository.TyomahdollisuusRepository;
import fi.okm.jod.yksilo.repository.TyopaikkaRepository;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.AbstractServiceTest;
import fi.okm.jod.yksilo.service.NotFoundException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Import({
  JakolinkkiService.class,
  YksiloService.class,
  YksilonSuosikkiService.class,
})
@Execution(ExecutionMode.SAME_THREAD)
class JakolinkkiServiceTest extends AbstractServiceTest {

  @Autowired JakolinkkiService jakolinkkiService;
  @Autowired YksilonSuosikkiService suosikkiService;

  @Autowired YksiloRepository yksiloRepository;
  @Autowired TyomahdollisuusRepository tyomahdollisuusRepository;
  @Autowired KoulutusmahdollisuusRepository koulutusmahdollisuusRepository;
  @Autowired TyopaikkaRepository tyopaikkaRepository;
  @Autowired KoulutusKokonaisuusRepository koulutusKokonaisuusRepository;
  @Autowired ToimintoRepository toimintoRepository;
  @Autowired TavoiteRepository tavoiteRepository;

  private Instant sixMonthsFromNowEod() {
    return LocalDate.now()
        .plusMonths(6)
        .atTime(23, 59, 59)
        .atZone(ZoneId.systemDefault())
        .toInstant();
  }

  private JakolinkkiUpdateDto defaultDto() {
    return new JakolinkkiUpdateDto(
        null,
        null,
        "Jakolinkki",
        sixMonthsFromNowEod(),
        "Muistiinpanoja",
        false,
        false,
        false,
        false,
        false,
        false,
        Set.of(),
        Set.of(),
        Set.of(),
        Set.of(),
        Set.of());
  }

  private Yksilo getYksilo() {
    return yksiloRepository.getReferenceById(user.getId());
  }

  private Yksilo getYksiloUser2() {
    return yksiloRepository.getReferenceById(user2.getId());
  }

  private void addTyopaikka(String fiName) {
    tyopaikkaRepository.save(new Tyopaikka(getYksilo(), ls(Kieli.FI, fiName)));
  }

  private void addKoulutusKokonaisuus(String fiName) {
    koulutusKokonaisuusRepository.save(new KoulutusKokonaisuus(getYksilo(), ls(Kieli.FI, fiName)));
  }

  private void addToiminto(String fiName) {
    toimintoRepository.save(new Toiminto(getYksilo(), ls(Kieli.FI, fiName)));
  }

  private void addTavoite(String fiName, Tyomahdollisuus tyomahdollisuus) {
    tavoiteRepository.save(
        new Tavoite(
            getYksilo(), tyomahdollisuus, ls(Kieli.FI, fiName), ls(Kieli.FI, fiName + " kuvaus")));
  }

  private UUID ulkoinenIdOfSingleLink() {
    var list = jakolinkkiService.list(user);
    assertThat(list).hasSize(1);
    return list.getFirst().ulkoinenId();
  }

  private UUID singleLinkId() {
    var list = jakolinkkiService.list(user);
    assertThat(list).hasSize(1);
    return list.getFirst().id();
  }

  private JakolinkkiUpdateDto dtoWithIds(UUID id, UUID ulkoinenId) {
    return defaultDto().withId(id).withUlkoinenId(ulkoinenId);
  }

  private void createJakolinkki(JakolinkkiUpdateDto dto) {
    jakolinkkiService.create(user, dto);
    simulateCommit();
  }

  @Test
  void shouldCreateJakolinkkiWithNoSharedFields() {

    var expectedVoimassaAsti = sixMonthsFromNowEod();
    jakolinkkiService.create(user, defaultDto().withVoimassaAsti(expectedVoimassaAsti));

    var jakolinkit = jakolinkkiService.list(user);
    assertEquals(1, jakolinkit.size());
    assertNotNull(jakolinkit.getFirst().id());
    assertNotNull(jakolinkit.getFirst().ulkoinenId());
    assertEquals("Jakolinkki", jakolinkit.getFirst().nimi());
    assertEquals(expectedVoimassaAsti, jakolinkit.getFirst().voimassaAsti());
    assertEquals("Muistiinpanoja", jakolinkit.getFirst().muistiinpano());

    var ulkoinenId = jakolinkit.getFirst().ulkoinenId();
    var jakolinkkiId = jakolinkit.getFirst().id();
    var jakolinkki = jakolinkkiService.get(user, jakolinkkiId);
    assertEquals(jakolinkkiId, jakolinkki.id());
    assertEquals(ulkoinenId, jakolinkki.ulkoinenId());
    assertEquals("Jakolinkki", jakolinkki.nimi());
    assertEquals(expectedVoimassaAsti, jakolinkki.voimassaAsti());
    assertEquals("Muistiinpanoja", jakolinkki.muistiinpano());
    assertNothingIsShared(jakolinkki);

    assertNoContentIsShared(jakolinkkiService.getContent(ulkoinenId), expectedVoimassaAsti);
  }

  private void assertNothingIsShared(JakolinkkiUpdateDto jakolinkki) {
    assertFalse(jakolinkki.emailJaettu());
    assertFalse(jakolinkki.nimiJaettu());
    assertFalse(jakolinkki.kiinnostuksetJaettu());
    assertFalse(jakolinkki.kotikuntaJaettu());
    assertFalse(jakolinkki.muuOsaaminenJaettu());
    assertEquals(0, jakolinkki.jaetutKoulutukset().size());
    assertEquals(0, jakolinkki.jaetutTyopaikat().size());
    assertEquals(0, jakolinkki.jaetutToiminnot().size());
    assertEquals(0, jakolinkki.jaetutSuosikit().size());
  }

  private void assertNoContentIsShared(JakolinkkiContentDto content, Instant expectedVoimassaAsti) {
    assertNotNull(content);
    assertEquals(expectedVoimassaAsti, content.voimassaAsti());
    assertNull(content.email());
    assertNull(content.etunimi());
    assertNull(content.sukunimi());
    assertNull(content.kotikunta());
    assertNull(content.kiinnostukset());
    assertNull(content.muuOsaaminen());
    assertEquals(0, content.koulutusKokonaisuudet().size());
    assertEquals(0, content.tyopaikat().size());
    assertEquals(0, content.toiminnot().size());
    assertEquals(0, content.suosikit().size());
  }

  @Test
  void shouldUpdateJakolinkki() {
    createJakolinkki(defaultDto());

    var jakolinkit = jakolinkkiService.list(user);
    assertEquals(1, jakolinkit.size());
    var ulkoinenId = jakolinkit.getFirst().ulkoinenId();
    var jakolinkkiId = jakolinkit.getFirst().id();

    var newVoimassaAsti =
        LocalDate.now()
            .plusMonths(12)
            .atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault())
            .toInstant();

    jakolinkkiService.update(
        user,
        defaultDto()
            .withId(jakolinkkiId)
            .withUlkoinenId(ulkoinenId)
            .withVoimassaAsti(newVoimassaAsti)
            .withNimi("Uusi Jakolinkki")
            .withMuistiinpano("Uudet muistiinpanot"));

    var updated = jakolinkkiService.get(user, jakolinkkiId);
    assertEquals(jakolinkkiId, updated.id());
    assertEquals("Uusi Jakolinkki", updated.nimi());
    assertEquals(newVoimassaAsti, updated.voimassaAsti());
    assertEquals("Uudet muistiinpanot", updated.muistiinpano());
  }

  @Test
  void shouldDeleteJakolinkki() {
    createJakolinkki(defaultDto());

    var jakolinkit = jakolinkkiService.list(user);
    assertEquals(1, jakolinkit.size());
    var ulkoinenId = jakolinkit.getFirst().ulkoinenId();

    jakolinkkiService.delete(user, jakolinkit.getFirst().id());

    var after = jakolinkkiService.list(user);
    assertEquals(0, after.size());

    assertThrows(NotFoundException.class, () -> jakolinkkiService.getContent(ulkoinenId));
  }

  @Test
  void shouldThrowNotFoundWhenGettingNonExistingJakolinkki() {
    var nonExistingUlkoinenId = UUID.randomUUID();
    assertThrows(
        NotFoundException.class, () -> jakolinkkiService.getContent(nonExistingUlkoinenId));
  }

  @Test
  void shouldThrowNotFoundWhenJakolinkkiHasExpired() {
    var expired =
        LocalDate.now().minusDays(1).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

    createJakolinkki(defaultDto().withVoimassaAsti(expired));

    var ulkoinenId = ulkoinenIdOfSingleLink();
    assertThrows(NotFoundException.class, () -> jakolinkkiService.getContent(ulkoinenId));
  }

  @Test
  void shouldBeValidWhenVoimassaAstiIsExactlyTodayEod() {
    var eod = LocalDate.now().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

    createJakolinkki(defaultDto().withVoimassaAsti(eod));

    var ulkoinenId = ulkoinenIdOfSingleLink();
    assertDoesNotThrow(() -> jakolinkkiService.getContent(ulkoinenId));
  }

  @Test
  void shouldBePossibleToRenew() {
    var expired =
        LocalDate.now().minusDays(1).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant();

    createJakolinkki(defaultDto().withVoimassaAsti(expired));
    var id = singleLinkId();
    var ulkoinenId = ulkoinenIdOfSingleLink();
    assertThrows(NotFoundException.class, () -> jakolinkkiService.getContent(ulkoinenId));

    var renewedVoimassaAsti = sixMonthsFromNowEod();
    jakolinkkiService.update(
        user, dtoWithIds(id, ulkoinenId).withVoimassaAsti(renewedVoimassaAsti));

    assertDoesNotThrow(() -> jakolinkkiService.getContent(ulkoinenId));
  }

  @Test
  void shouldShareUserBasicInformation() {
    yksiloRepository.updateName(user.getQualifiedPersonId(), user.givenName(), user.familyName());
    yksiloRepository.updateEmail(user.getQualifiedPersonId(), "test@example.com");
    var yksilo = yksiloRepository.findById(user.id()).orElseThrow();
    yksilo.setKotikunta("934");
    yksilo.setSyntymavuosi(1980);
    yksiloRepository.save(yksilo);

    var expectedVoimassaAsti = sixMonthsFromNowEod();
    createJakolinkki(
        defaultDto()
            .withVoimassaAsti(expectedVoimassaAsti)
            .withNimiJaettu(true)
            .withEmailJaettu(true)
            .withKotikuntaJaettu(true)
            .withSyntymavuosiJaettu(true));

    var ulkoinenId = ulkoinenIdOfSingleLink();
    var jakolinkkiId = singleLinkId();

    var jakolinkki = jakolinkkiService.get(user, jakolinkkiId);
    assertEquals("Jakolinkki", jakolinkki.nimi());
    assertEquals(expectedVoimassaAsti, jakolinkki.voimassaAsti());
    assertTrue(jakolinkki.nimiJaettu());
    assertTrue(jakolinkki.emailJaettu());

    var content = jakolinkkiService.getContent(ulkoinenId);
    assertNotNull(content);
    assertEquals(user.givenName(), content.etunimi());
    assertEquals(user.familyName(), content.sukunimi());
    assertEquals("test@example.com", content.email());
    assertEquals(1980, content.syntymavuosi());
    assertEquals("934", content.kotikunta());
  }

  @Test
  void shouldStopSharingNameAndEmail() {
    yksiloRepository.updateName(user.getQualifiedPersonId(), user.givenName(), user.familyName());
    yksiloRepository.updateEmail(user.getQualifiedPersonId(), "test@example.com");

    createJakolinkki(defaultDto().withNimiJaettu(true).withEmailJaettu(true));
    var id = singleLinkId();
    var ext = ulkoinenIdOfSingleLink();

    var before = jakolinkkiService.getContent(ext);
    assertThat(before.email()).isEqualTo("test@example.com");

    jakolinkkiService.update(
        user, dtoWithIds(id, ext).withNimiJaettu(false).withEmailJaettu(false));

    var after = jakolinkkiService.getContent(ext);
    assertThat(after.email()).isNull();
    assertThat(after.etunimi()).isNull();
  }

  @Test
  void shouldStopSharingTyopaikat() {
    addTyopaikka("A");
    addTyopaikka("B");
    var all = tyopaikkaRepository.findByYksiloId(user.id());
    simulateCommit();

    createJakolinkki(
        defaultDto().withJaetutTyopaikat(all.stream().map(Tyopaikka::getId).collect(toSet())));

    var id = singleLinkId();
    var ext = ulkoinenIdOfSingleLink();
    var before = jakolinkkiService.getContent(ext);
    assertThat(before.tyopaikat()).hasSize(2);

    jakolinkkiService.update(user, dtoWithIds(id, ext).withJaetutTyopaikat(Set.of()));

    var after = jakolinkkiService.getContent(ext);
    assertThat(after.tyopaikat()).isEmpty();
  }

  @Test
  void shouldShareSelectedTyopaikat() {
    addTyopaikka("Tyopaikka 1");
    addTyopaikka("Tyopaikka 2");
    var tyopaikat = tyopaikkaRepository.findByYksiloId(user.id());
    assertThat(tyopaikat).hasSize(2);
    simulateCommit();

    createJakolinkki(
        defaultDto()
            .withJaetutTyopaikat(tyopaikat.stream().map(Tyopaikka::getId).collect(toSet())));

    var ulkoinenId = ulkoinenIdOfSingleLink();

    var content = jakolinkkiService.getContent(ulkoinenId);
    assertThat(content.tyopaikat())
        .extracting(tp -> tp.nimi().get(Kieli.FI))
        .containsExactlyInAnyOrder("Tyopaikka 1", "Tyopaikka 2");

    var jakolinkkiId = singleLinkId();

    jakolinkkiService.update(
        user,
        dtoWithIds(jakolinkkiId, ulkoinenId)
            .withJaetutTyopaikat(Set.of(tyopaikat.getFirst().getId())));

    var after = jakolinkkiService.getContent(ulkoinenId);
    assertThat(after.tyopaikat())
        .extracting(tp -> tp.nimi().get(Kieli.FI))
        .containsExactly("Tyopaikka 1");
  }

  @Test
  void shouldShareSelectedKoulutusKokonaisuudet() {
    addKoulutusKokonaisuus("Koulutus 1");
    addKoulutusKokonaisuus("Koulutus 2");
    var koulutukset = koulutusKokonaisuusRepository.findByYksiloId(user.id());
    assertThat(koulutukset).hasSize(2);
    simulateCommit();

    createJakolinkki(
        defaultDto()
            .withJaetutKoulutukset(
                koulutukset.stream().map(KoulutusKokonaisuus::getId).collect(toSet())));

    var ulkoinenId = ulkoinenIdOfSingleLink();

    var content = jakolinkkiService.getContent(ulkoinenId);
    assertThat(content.koulutusKokonaisuudet())
        .extracting(kk -> kk.nimi().get(Kieli.FI))
        .containsExactlyInAnyOrder("Koulutus 1", "Koulutus 2");

    var jakolinkkiId = singleLinkId();

    jakolinkkiService.update(
        user,
        dtoWithIds(jakolinkkiId, ulkoinenId)
            .withJaetutKoulutukset(Set.of(koulutukset.getLast().getId())));

    var after = jakolinkkiService.getContent(ulkoinenId);
    assertThat(after.koulutusKokonaisuudet())
        .extracting(kk -> kk.nimi().get(Kieli.FI))
        .containsExactly("Koulutus 2");
  }

  @Test
  void shouldShareSelectedToiminnot() {
    addToiminto("Toiminto 1");
    addToiminto("Toiminto 2");
    var toiminnot = toimintoRepository.findByYksiloId(user.id());
    assertThat(toiminnot).hasSize(2);
    simulateCommit();

    createJakolinkki(
        defaultDto()
            .withJaetutToiminnot(
                toiminnot.stream().map(Toiminto::getId).collect(Collectors.toSet())));

    var ulkoinenId = ulkoinenIdOfSingleLink();

    var content = jakolinkkiService.getContent(ulkoinenId);
    assertThat(content.toiminnot())
        .extracting(t -> t.nimi().get(Kieli.FI))
        .containsExactlyInAnyOrder("Toiminto 1", "Toiminto 2");

    var jakolinkkiId = singleLinkId();

    jakolinkkiService.update(
        user,
        dtoWithIds(jakolinkkiId, ulkoinenId)
            .withJaetutToiminnot(Set.of(toiminnot.getFirst().getId())));

    var after = jakolinkkiService.getContent(ulkoinenId);
    assertThat(after.toiminnot())
        .extracting(t -> t.nimi().get(Kieli.FI))
        .containsExactly("Toiminto 1");
  }

  @Test
  @Sql("/data/mahdollisuudet-test-data.sql")
  void shouldShareSelectedSuosikit() {
    var tyomahdollisuusIds =
        tyomahdollisuusRepository.findAll().stream().map(Tyomahdollisuus::getId).toList();
    var koulutusmahdollisuusIds =
        koulutusmahdollisuusRepository.findAll().stream().map(Koulutusmahdollisuus::getId).toList();

    var tyo1 = tyomahdollisuusIds.getFirst();
    var tyo2 = tyomahdollisuusIds.get(1);
    var kou1 = koulutusmahdollisuusIds.getFirst();
    var kou2 = koulutusmahdollisuusIds.get(1);

    suosikkiService.add(user, tyo1, SuosikkiTyyppi.TYOMAHDOLLISUUS);
    suosikkiService.add(user, tyo2, SuosikkiTyyppi.TYOMAHDOLLISUUS);
    suosikkiService.add(user, kou1, SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS);
    suosikkiService.add(user, kou2, SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS);

    simulateCommit();

    createJakolinkki(
        defaultDto()
            .withJaetutSuosikit(
                Set.of(SuosikkiTyyppi.TYOMAHDOLLISUUS, SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS)));

    var ulkoinenId = ulkoinenIdOfSingleLink();
    var jakolinkkiId = singleLinkId();

    var content = jakolinkkiService.getContent(ulkoinenId);
    assertNotNull(content);
    assertThat(content.suosikit())
        .extracting(s -> Map.entry(s.tyyppi(), s.kohdeId()))
        .containsExactlyInAnyOrder(
            Map.entry(SuosikkiTyyppi.TYOMAHDOLLISUUS, tyo1),
            Map.entry(SuosikkiTyyppi.TYOMAHDOLLISUUS, tyo2),
            Map.entry(SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS, kou1),
            Map.entry(SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS, kou2));

    jakolinkkiService.update(
        user,
        dtoWithIds(jakolinkkiId, ulkoinenId)
            .withJaetutSuosikit(Set.of(SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS)));

    var after = jakolinkkiService.getContent(ulkoinenId);
    assertNotNull(after);
    assertThat(after.suosikit())
        .extracting(s -> Map.entry(s.tyyppi(), s.kohdeId()))
        .containsExactlyInAnyOrder(
            Map.entry(SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS, kou1),
            Map.entry(SuosikkiTyyppi.KOULUTUSMAHDOLLISUUS, kou2));
  }

  @Test
  void shouldShareMuuOsaaminen() {
    var yksilo = getYksilo();
    var muuOsaaminen = new MuuOsaaminen(yksilo, Collections.emptySet());
    entityManager.persist(
        new YksilonOsaaminen(muuOsaaminen, entityManager.find(Osaaminen.class, 1L)));
    yksilo.setMuuOsaaminenVapaateksti(ls(Kieli.FI, "Muun osaamisen vapaateksti"));
    yksiloRepository.save(yksilo);

    createJakolinkki(defaultDto().withMuuOsaaminenJaettu(true));

    var ulkoinenId = ulkoinenIdOfSingleLink();
    var content = jakolinkkiService.getContent(ulkoinenId);
    assertNotNull(content);
    assertThat(content.muuOsaaminen()).isNotNull();
    assertThat(content.muuOsaaminen().vapaateksti().get(Kieli.FI))
        .isEqualTo("Muun osaamisen vapaateksti");
    assertThat(content.muuOsaaminen().muuOsaaminen())
        .containsExactly(URI.create("urn:osaaminen:1"));
  }

  @Test
  void shouldShareKiinnostukset() {
    var osaaminen1 = entityManager.find(Osaaminen.class, 1L);
    var osaaminen2 = entityManager.find(Osaaminen.class, 2L);
    var ammatti1 = entityManager.find(Ammatti.class, 1L);
    var ammatti2 = entityManager.find(Ammatti.class, 2L);

    var yksilo = getYksilo();
    yksilo.getOsaamisKiinnostukset().add(osaaminen1);
    yksilo.getOsaamisKiinnostukset().add(osaaminen2);
    yksilo.getAmmattiKiinnostukset().add(ammatti1);
    yksilo.getAmmattiKiinnostukset().add(ammatti2);
    yksilo.setOsaamisKiinnostuksetVapaateksti(ls(Kieli.FI, "Osaamiskiinnostusten vapaateksti"));
    yksiloRepository.save(yksilo);

    createJakolinkki(defaultDto().withKiinnostuksetJaettu(true));

    var ulkoinenId = ulkoinenIdOfSingleLink();
    var content = jakolinkkiService.getContent(ulkoinenId);
    assertNotNull(content);
    assertThat(content.kiinnostukset()).isNotNull();
    assertThat(content.kiinnostukset().vapaateksti().get(Kieli.FI))
        .isEqualTo("Osaamiskiinnostusten vapaateksti");
    assertThat(content.kiinnostukset().kiinnostukset())
        .containsExactlyInAnyOrder(
            URI.create("urn:osaaminen:1"),
            URI.create("urn:osaaminen:2"),
            URI.create("urn:ammatti:1"),
            URI.create("urn:ammatti:2"));
  }

  @Test
  @Sql("/data/mahdollisuudet-test-data.sql")
  void shouldShareTavoitteet() {
    var tyomahdollisuus = tyomahdollisuusRepository.findAll().stream().findFirst().orElseThrow();
    var yksilo = getYksilo();
    addTavoite("Tavoite 1", tyomahdollisuus);

    var tavoitteet = tavoiteRepository.findAllByYksilo(yksilo);
    assertThat(tavoitteet).hasSize(1);
    simulateCommit();
    createJakolinkki(
        defaultDto()
            .withJaetutTavoitteet(
                tavoitteet.stream().map(Tavoite::getId).collect(Collectors.toSet())));

    var ulkoinenId = ulkoinenIdOfSingleLink();
    var content = jakolinkkiService.getContent(ulkoinenId);
    assertNotNull(content);
    assertThat(content.tavoitteet()).hasSize(1);

    assertThat(content.tavoitteet())
        .extracting(t -> t.tavoite().get(Kieli.FI))
        .containsExactlyInAnyOrder("Tavoite 1");

    assertThat(content.tavoitteet())
        .extracting(TavoiteDto::mahdollisuusId)
        .containsExactlyInAnyOrder(tyomahdollisuus.getId());
  }

  @Test
  void shouldNotShowLinkToAnotherUser() {
    createJakolinkki(defaultDto());
    var ext = ulkoinenIdOfSingleLink();

    assertThrows(NotFoundException.class, () -> jakolinkkiService.get(user2, ext));
  }

  @Test
  void shouldNotAllowSharingItemsOwnedByAnotherUser() {

    tyopaikkaRepository.save(new Tyopaikka(getYksiloUser2(), ls(Kieli.FI, "Vieraan Työ")));

    addTyopaikka("Oma Työ");
    var own = tyopaikkaRepository.findByYksiloId(user.id());
    var foreign = tyopaikkaRepository.findByYksiloId(user2.getId());

    simulateCommit();

    createJakolinkki(
        defaultDto()
            .withJaetutTyopaikat(Set.of(own.getFirst().getId(), foreign.getFirst().getId())));

    var content = jakolinkkiService.getContent(ulkoinenIdOfSingleLink());
    assertThat(content.tyopaikat())
        .extracting(tp -> tp.nimi().get(Kieli.FI))
        .containsExactly("Oma Työ");
  }

  @Test
  void shouldNotBreakWhenFlagsOnButValuesMissing() {

    createJakolinkki(
        defaultDto()
            .withNimiJaettu(true)
            .withEmailJaettu(true)
            .withKotikuntaJaettu(true)
            .withSyntymavuosiJaettu(true));

    var content = jakolinkkiService.getContent(ulkoinenIdOfSingleLink());
    assertNull(content.etunimi());
    assertNull(content.sukunimi());
    assertNull(content.email());
    assertNull(content.kotikunta());
    assertNull(content.syntymavuosi());
  }

  @Test
  void shouldDeleteJakolinkkiWhenUserDeleted() {
    createJakolinkki(defaultDto());
    var jakolinkkiId = singleLinkId();
    simulateCommit();

    yksiloRepository.deleteById(user.getId());
    yksiloRepository.removeId(user.getId());

    assertThrows(NotFoundException.class, () -> jakolinkkiService.get(user, jakolinkkiId));
  }
}
