/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.TuontiLahde;
import fi.okm.jod.yksilo.dto.profiili.KoulutusDto;
import fi.okm.jod.yksilo.dto.profiili.KoulutusKokonaisuusDto;
import fi.okm.jod.yksilo.service.koski.KoskiExtractionRequest.Koulutuskokonaisuus;
import fi.okm.jod.yksilo.service.koski.KoskiExtractionRequest.Laajuus;
import fi.okm.jod.yksilo.service.koski.KoskiExtractionRequest.Opinto;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.MissingNode;

/**
 * Reads the study rights of a Koski omadata response into educations, and what of them goes to
 * skill extraction. Personal data (henkilö) and grades are not read.
 *
 * @see <a
 *     href="https://virkailija.opintopolku.fi/koski/json-schema-viewer/?schema=omadata-oauth2-kaikki-tiedot-oppija-schema.json">Koski
 *     schema</a>
 */
@Component
@RequiredArgsConstructor
@Slf4j
class KoskiReader {

  /** The educations to import, and those of them whose skills are extracted. */
  record Educations(
      List<KoulutusKokonaisuusDto> koulutuskokonaisuudet, List<Koulutuskokonaisuus> extraction) {}

  /** Steps towards the oppimäärä in the same study right, not educations of their own. */
  private static final String GRADE_LEVEL = "perusopetuksenvuosiluokka";

  /**
   * Study rights of general education. Skills derived from their general subjects are too broad to
   * be useful, so they are not extracted.
   */
  private static final Set<String> GENERAL_EDUCATION =
      Set.of(
          "esiopetus",
          "perusopetus",
          "aikuistenperusopetus",
          "perusopetukseenvalmistavaopetus",
          "perusopetuksenlisaopetus",
          "lukiokoulutus",
          "luva",
          "ylioppilastutkinto",
          "ibtutkinto",
          "diatutkinto",
          "ebtutkinto",
          "internationalschool",
          "europeanschoolofhelsinki");

  /** Higher education courses outside any degree; one education collects them. */
  private static final String LOOSE_COURSE = "korkeakoulunopintojakso";

  private static final LocalizedString LOOSE_COURSES_NAME =
      new LocalizedString(
          Map.of(Kieli.FI, "Opintojaksot", Kieli.SV, "Studieavsnitt", Kieli.EN, "Courses"));

  /** Virta codes some studies only by the kind of study right ("Avoimen opinnot"). */
  private static final String STUDY_RIGHT_TYPE = "virtaopiskeluoikeudentyyppi";

  /**
   * Education codes that name only the kind of education ("Muu kuin säännelty koulutus", "Vapaan
   * sivistystyön koulutus"); what was studied is in the opintokokonaisuus.
   */
  private static final Set<String> GENERIC_EDUCATION = Set.of("999951", "099999");

  private static final List<Kieli> LANGUAGES = List.of(Kieli.FI, Kieli.SV, Kieli.EN);
  private static final Pattern WHITESPACE = Pattern.compile("(?U)\\s+");

  private final Validator validator;

  /**
   * One kokonaisuus per study right that names its institution and holds studies, related ones side
   * by side. The educations have no osaamiset yet.
   */
  Educations read(JsonNode koski) {
    var kokonaisuudet = new ArrayList<KoulutusKokonaisuusDto>();
    var extraction = new ArrayList<Koulutuskokonaisuus>();
    var named =
        list(koski.path("opiskeluoikeudet")).stream()
            .filter(KoskiReader::namesInstitution)
            .toList();
    for (var opiskeluoikeus : inOrder(named)) {
      var institution = localized(toimija(opiskeluoikeus).path("nimi"));
      var general =
          GENERAL_EDUCATION.contains(opiskeluoikeus.path("tyyppi").path("koodiarvo").asString(""));
      var koulutukset = new LinkedHashSet<KoulutusDto>();
      var extracted = new ArrayList<KoskiExtractionRequest.Koulutus>();
      for (var education : educations(opiskeluoikeus, institution)) {
        var koulutus = education.toDto();
        if (!isValid(koulutus)) {
          continue;
        }
        koulutukset.add(koulutus);
        if (education.hasData() && !general) {
          extracted.add(education.toRequest(koulutus.id()));
        }
      }
      if (koulutukset.isEmpty()) {
        continue;
      }
      var kokonaisuus =
          new KoulutusKokonaisuusDto(
              UUID.randomUUID(), institution, TuontiLahde.KOSKI_TUONTI, koulutukset);
      if (!isValid(kokonaisuus)) {
        continue;
      }
      kokonaisuudet.add(kokonaisuus);
      if (!extracted.isEmpty()) {
        var jaksot = list(opiskeluoikeus.path("tila").path("opiskeluoikeusjaksot"));
        extraction.add(
            new Koulutuskokonaisuus(
                institutionKey(opiskeluoikeus),
                institution,
                localized(opiskeluoikeus.path("oppilaitos").path("oppilaitostyyppi").path("nimi")),
                localized(opiskeluoikeus.path("tyyppi").path("nimi")),
                date(opiskeluoikeus.path("alkamispäivä")),
                date(opiskeluoikeus.path("päättymispäivä")),
                jaksot.isEmpty() ? null : localized(jaksot.getLast().path("tila").path("nimi")),
                extracted));
      }
    }
    return new Educations(kokonaisuudet, extraction);
  }

  private <T> boolean isValid(T dto) {
    var violations = validator.validate(dto);
    if (!violations.isEmpty()) {
      log.atWarn()
          .addKeyValue(
              "validationErrors",
              violations.stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).toList())
          .log("Invalid data in Koski response, ignored");
    }
    return violations.isEmpty();
  }

  /** One päätason suoritus, or the loose courses of a study right; with its studies. */
  private record Education(
      LocalizedString nimi,
      LocalDate alkuPvm,
      LocalDate loppuPvm,
      boolean hasData,
      JsonNode suoritus,
      List<Opinto> opinnot) {

    KoulutusDto toDto() {
      return new KoulutusDto(UUID.randomUUID(), nimi, null, alkuPvm, loppuPvm, Set.of());
    }

    KoskiExtractionRequest.Koulutus toRequest(UUID id) {
      var moduuli = suoritus.path("koulutusmoduuli");
      return new KoskiExtractionRequest.Koulutus(
          id,
          nimi,
          localized(suoritus.path("tyyppi").path("nimi")),
          localized(moduuli.path("virtaNimi")),
          localized(moduuli.path("kuvaus")),
          list(suoritus.path("tutkintonimike")).stream()
              .map(t -> localized(t.path("nimi")))
              .filter(Objects::nonNull)
              .toList(),
          list(suoritus.path("osaamisala")).stream()
              .map(t -> localized(t.path("osaamisala").path("nimi")))
              .filter(Objects::nonNull)
              .toList(),
          opinnot);
    }
  }

  /**
   * One education per päätason suoritus, except that grade levels are dropped and loose courses go
   * together. Loose courses belong to the only other suoritus of the study right if there is one.
   */
  private static List<Education> educations(JsonNode opiskeluoikeus, LocalizedString institution) {
    var suoritukset = list(opiskeluoikeus.path("suoritukset"));
    if (suoritukset.stream().anyMatch(s -> !GRADE_LEVEL.equals(type(s)))) {
      suoritukset = suoritukset.stream().filter(s -> !GRADE_LEVEL.equals(type(s))).toList();
    } else if (!suoritukset.isEmpty()) {
      // Only grade levels, e.g. basic education left unfinished: the latest stands for them.
      JsonNode latest = null;
      for (var s : suoritukset) {
        if (latest == null || !confirmedOrMax(s).isBefore(confirmedOrMax(latest))) {
          latest = s;
        }
      }
      suoritukset = List.of(latest);
    }
    var start = date(opiskeluoikeus.path("alkamispäivä"));
    var end = date(opiskeluoikeus.path("päättymispäivä"));
    var loose = suoritukset.stream().filter(s -> LOOSE_COURSE.equals(type(s))).toList();
    var others = suoritukset.stream().filter(s -> !LOOSE_COURSE.equals(type(s))).toList();
    // Virta often reports a degree's courses beside it rather than under it.
    var attached = others.size() == 1 ? loose : List.<JsonNode>of();

    var result = new ArrayList<Education>();
    for (var suoritus : others) {
      // The institution stands in when Koski gives no name.
      var nimi =
          or(
              or(moduleName(suoritus), localized(suoritus.path("tyyppi").path("nimi"))),
              institution);
      var confirmed = vahvistus(suoritus) != null;
      var studies = concat(list(suoritus.path("osasuoritukset")), attached);
      result.add(
          new Education(
              nimi,
              start,
              or(vahvistus(suoritus), end),
              hasData(suoritus) || !attached.isEmpty(),
              suoritus,
              opinnot(studies, confirmed)));
    }
    if (!loose.isEmpty() && attached.isEmpty()) {
      var completed =
          loose.stream()
              .map(KoskiReader::vahvistus)
              .filter(Objects::nonNull)
              .max(Comparator.naturalOrder());
      result.add(
          new Education(
              LOOSE_COURSES_NAME,
              start,
              completed.orElse(end),
              loose.stream().anyMatch(KoskiReader::hasData),
              MissingNode.getInstance(),
              opinnot(loose, false)));
    }
    return result;
  }

  private static List<Opinto> opinnot(List<JsonNode> suoritukset, boolean confirmed) {
    return suoritukset.stream().flatMap(s -> opinto(s, confirmed).stream()).toList();
  }

  /** The study, or the studies under it when it has no name. */
  private static List<Opinto> opinto(JsonNode suoritus, boolean confirmed) {
    var children = opinnot(list(suoritus.path("osasuoritukset")), confirmed);
    var nimi = moduleName(suoritus);
    if (nimi == null) {
      return children;
    }
    var moduuli = suoritus.path("koulutusmoduuli");
    var laajuus = moduuli.path("laajuus");
    var arvo = laajuus.path("arvo");
    var yksikko = laajuus.path("yksikkö");
    return List.of(
        new Opinto(
            nimi,
            localized(moduuli.path("kieli").path("nimi")),
            arvo.isNumber()
                ? new Laajuus(
                    arvo.numberValue(),
                    or(localized(yksikko.path("lyhytNimi")), localized(yksikko.path("nimi"))))
                : null,
            // Some studies are never graded, so only those under an unconfirmed one are unfinished.
            !truthy(suoritus.path("arviointi")) && !confirmed,
            localized(moduuli.path("kuvaus")),
            children));
  }

  /** Whether the suoritus says what was studied: a qualification, its own name, or studies. */
  private static boolean hasData(JsonNode suoritus) {
    var moduuli = suoritus.path("koulutusmoduuli");
    var tunniste = moduuli.path("tunniste");
    var codeName = localized(tunniste.path("nimi"));
    var namesQualification =
        codeName != null
            && !STUDY_RIGHT_TYPE.equals(tunniste.path("koodistoUri").asString(null))
            && !GENERIC_EDUCATION.contains(tunniste.path("koodiarvo").asString(""));
    // Some systems repeat the code's name as the module name, which adds nothing.
    var ownName = ownName(moduuli);
    var hasOwnName =
        ownName != null
            && ownName.asMap().entrySet().stream()
                .anyMatch(e -> codeName == null || !e.getValue().equals(codeName.get(e.getKey())));
    return namesQualification
        || hasOwnName
        || localized(moduuli.path("kuvaus")) != null
        || localized(moduuli.path("virtaNimi")) != null
        || !list(suoritus.path("osasuoritukset")).isEmpty()
        || !list(suoritus.path("tutkintonimike")).isEmpty()
        || !list(suoritus.path("osaamisala")).isEmpty();
  }

  private static LocalizedString moduleName(JsonNode suoritus) {
    var moduuli = suoritus.path("koulutusmoduuli");
    return join(localized(moduuli.path("tunniste").path("nimi")), ownName(moduuli));
  }

  /**
   * The module's own name or its opintokokonaisuus, without identifiers some systems put there
   * (e.g. "urn:...").
   */
  private static LocalizedString ownName(JsonNode moduuli) {
    var name =
        or(
            localized(moduuli.path("nimi")),
            localized(moduuli.path("opintokokonaisuus").path("nimi")));
    if (name == null) {
      return null;
    }
    var values = new EnumMap<Kieli, String>(Kieli.class);
    name.asMap()
        .forEach(
            (k, v) -> {
              if (!v.toLowerCase().startsWith("urn:")) {
                values.put(k, v);
              }
            });
    return values.isEmpty() ? null : new LocalizedString(values);
  }

  /** "Tutkinto: nimi" in each language of the title. */
  static LocalizedString join(LocalizedString title, LocalizedString name) {
    if (title == null || name == null) {
      return title == null ? name : title;
    }
    var values = new EnumMap<Kieli, String>(Kieli.class);
    title
        .asMap()
        .forEach(
            (kieli, text) -> {
              var own = name.get(kieli);
              values.put(kieli, own != null && !own.equals(text) ? text + ": " + own : text);
            });
    return new LocalizedString(values);
  }

  private static JsonNode toimija(JsonNode opiskeluoikeus) {
    var oppilaitos = opiskeluoikeus.path("oppilaitos");
    return truthy(oppilaitos) ? oppilaitos : opiskeluoikeus.path("koulutustoimija");
  }

  private static boolean namesInstitution(JsonNode opiskeluoikeus) {
    if (localized(toimija(opiskeluoikeus).path("nimi")) == null) {
      log.info("Skipping a Koski opiskeluoikeus with no institution name");
      return false;
    }
    return true;
  }

  private static String institutionKey(JsonNode opiskeluoikeus) {
    var toimija = toimija(opiskeluoikeus);
    var oid = toimija.path("oid").asString("");
    return oid.isEmpty() ? value(localized(toimija.path("nimi"))) : oid;
  }

  /**
   * Rights of one institution together, by start date; institutions by their earliest start. Koski
   * does not guarantee an order; this one keeps related rights side by side.
   */
  private static List<JsonNode> inOrder(List<JsonNode> opiskeluoikeudet) {
    var firstStart = new HashMap<String, LocalDate>();
    for (var o : opiskeluoikeudet) {
      firstStart.merge(institutionKey(o), startOrMax(o), (a, b) -> a.isBefore(b) ? a : b);
    }
    return opiskeluoikeudet.stream()
        .sorted(
            Comparator.comparing((JsonNode o) -> firstStart.get(institutionKey(o)))
                .thenComparing(KoskiReader::institutionKey)
                .thenComparing(KoskiReader::startOrMax))
        .toList();
  }

  private static LocalDate startOrMax(JsonNode opiskeluoikeus) {
    return Objects.requireNonNullElse(date(opiskeluoikeus.path("alkamispäivä")), LocalDate.MAX);
  }

  private static LocalDate confirmedOrMax(JsonNode suoritus) {
    return Objects.requireNonNullElse(vahvistus(suoritus), LocalDate.MAX);
  }

  private static String type(JsonNode suoritus) {
    return suoritus.path("tyyppi").path("koodiarvo").asString(null);
  }

  private static LocalDate vahvistus(JsonNode suoritus) {
    return date(suoritus.path("vahvistus").path("päivä"));
  }

  private static LocalDate date(JsonNode node) {
    if (!node.isString()) {
      return null;
    }
    try {
      return LocalDate.parse(node.asString());
    } catch (DateTimeParseException _) {
      return null;
    }
  }

  /** The objects of a JSON array; nothing for anything else. */
  private static List<JsonNode> list(JsonNode node) {
    return node.isArray() ? node.valueStream().filter(JsonNode::isObject).toList() : List.of();
  }

  private static <T> T or(T value, T fallback) {
    return value != null ? value : fallback;
  }

  private static <T> List<T> concat(List<T> a, List<T> b) {
    return Stream.concat(a.stream(), b.stream()).toList();
  }

  /** Whether a value is present and not empty, false or zero. */
  private static boolean truthy(JsonNode node) {
    if (node.isMissingNode() || node.isNull()) {
      return false;
    }
    if (node.isContainer()) {
      return !node.isEmpty();
    }
    if (node.isBoolean()) {
      return node.booleanValue();
    }
    if (node.isNumber()) {
      return node.doubleValue() != 0;
    }
    return !node.asString("").isEmpty();
  }

  /** The text in Finnish, else in the first other language it has. */
  private static String value(LocalizedString text) {
    if (text == null) {
      return null;
    }
    return LANGUAGES.stream().map(text::get).filter(Objects::nonNull).findFirst().orElse(null);
  }

  /** Each language's text on one line. */
  static LocalizedString localized(JsonNode node) {
    if (!node.isObject()) {
      return null;
    }
    var values = new LinkedHashMap<Kieli, String>();
    for (var kieli : LANGUAGES) {
      var text = node.path(kieli.getKoodi());
      if (text.isString() && !text.asString().isBlank()) {
        values.put(kieli, WHITESPACE.matcher(text.asString().strip()).replaceAll(" "));
      }
    }
    return values.isEmpty() ? null : new LocalizedString(values);
  }
}
