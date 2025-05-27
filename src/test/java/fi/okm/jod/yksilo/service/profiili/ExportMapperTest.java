/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili;

import static org.assertj.core.api.Assertions.assertThat;

import fi.okm.jod.yksilo.dto.profiili.export.KoulutusExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.KoulutusKokonaisuusExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.PaamaaraExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.PatevyysExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.PolunSuunnitelmaExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.PolunVaiheExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.ToimenkuvaExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.ToimintoExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.TyopaikkaExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.YksiloExportDto;
import fi.okm.jod.yksilo.dto.profiili.export.YksilonSuosikkiExportDto;
import fi.okm.jod.yksilo.entity.Koulutus;
import fi.okm.jod.yksilo.entity.KoulutusKokonaisuus;
import fi.okm.jod.yksilo.entity.Paamaara;
import fi.okm.jod.yksilo.entity.Patevyys;
import fi.okm.jod.yksilo.entity.PolunSuunnitelma;
import fi.okm.jod.yksilo.entity.PolunVaihe;
import fi.okm.jod.yksilo.entity.Toimenkuva;
import fi.okm.jod.yksilo.entity.Toiminto;
import fi.okm.jod.yksilo.entity.Tyopaikka;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.entity.YksilonSuosikki;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ExportMapperTest {

  @Test
  void testNoNewOrDeletedMappingMethods() {
    Set<String> expectedMethods =
        Set.of(
            "mapYksilo",
            "mapTyopaikka",
            "mapToimenkuva",
            "mapKoulutusKokonaisuus",
            "mapKoulutus",
            "mapToiminto",
            "mapPatevyys",
            "mapYksilonSuosikki",
            "mapPaamaara",
            "mapPolunSuunnitelma",
            "mapPolunVaihe");

    Set<String> actualMethods =
        Set.of(ExportMapper.class.getDeclaredMethods()).stream()
            .filter(
                method ->
                    Modifier.isPublic(method.getModifiers())
                        && Modifier.isStatic(method.getModifiers()))
            .map(Method::getName)
            .collect(Collectors.toSet());

    assertThat(actualMethods).containsExactlyInAnyOrderElementsOf(expectedMethods);
  }

  @Test
  void testYksiloMappingCompleteness() {
    assertMappingCompleteness(
        Yksilo.class, YksiloExportDto.class, Set.of("osaamiset", "tapahtumat"));
  }

  @Test
  void testTyopaikkaMappingCompleteness() {
    assertMappingCompleteness(Tyopaikka.class, TyopaikkaExportDto.class, Set.of("yksilo"));
  }

  @Test
  void testToimenkuvaMappingCompleteness() {
    assertMappingCompleteness(
        Toimenkuva.class, ToimenkuvaExportDto.class, Set.of("yksilo", "tyopaikka"));
  }

  @Test
  void testKoulutusKokonaisuusMappingCompleteness() {
    assertMappingCompleteness(
        KoulutusKokonaisuus.class, KoulutusKokonaisuusExportDto.class, Set.of("yksilo"));
  }

  @Test
  void testKoulutusMappingCompleteness() {
    assertMappingCompleteness(
        Koulutus.class, KoulutusExportDto.class, Set.of("yksilo", "kokonaisuus"));
  }

  @Test
  void testToimintoMappingCompleteness() {
    assertMappingCompleteness(Toiminto.class, ToimintoExportDto.class, Set.of("yksilo"));
  }

  @Test
  void testPatevyysMappingCompleteness() {
    assertMappingCompleteness(
        Patevyys.class, PatevyysExportDto.class, Set.of("yksilo", "toiminto"));
  }

  @Test
  void testYksilonSuosikkiMappingCompleteness() {
    assertMappingCompleteness(
        YksilonSuosikki.class, YksilonSuosikkiExportDto.class, Set.of("yksilo", "kohdeId"));
  }

  @Test
  void testPaamaaraMappingCompleteness() {
    assertMappingCompleteness(
        Paamaara.class,
        PaamaaraExportDto.class,
        Set.of("yksilo", "osaamiset", "mahdollisuusTyyppi", "mahdollisuusId"));
  }

  @Test
  void testPolunSuunnitelmaMappingCompleteness() {
    assertMappingCompleteness(
        PolunSuunnitelma.class, PolunSuunnitelmaExportDto.class, Set.of("yksilo", "paamaara"));
  }

  @Test
  void testPolunVaiheMappingCompleteness() {
    assertMappingCompleteness(
        PolunVaihe.class, PolunVaiheExportDto.class, Set.of("polunSuunnitelma"));
  }

  private void assertMappingCompleteness(
      Class<?> entityClass, Class<?> dtoClass, Set<String> ignoredGetters) {
    Set<String> entityGetters = getGetterNames(entityClass, ignoredGetters);
    Set<String> dtoGetters = getGetterNames(dtoClass, Set.of());
    assertThat(entityGetters).isSubsetOf(dtoGetters);
  }

  private Set<String> getGetterNames(Class<?> clazz, Set<String> ignoredGetters) {
    if (clazz.isRecord()) {
      return Set.of(clazz.getRecordComponents()).stream()
          .map(RecordComponent::getName)
          .filter(
              componentName -> !ignoredGetters.contains(componentName)) // Skip ignored components
          .collect(Collectors.toSet());
    } else {
      return Set.of(clazz.getDeclaredMethods()).stream()
          .filter(method -> Modifier.isPublic(method.getModifiers())) // Only public methods
          .filter(
              method ->
                  method.getName().startsWith("get")
                      || method.getName().startsWith("is")) // Getter methods
          .map(method -> method.getName().replaceFirst("^(get|is)", "")) // Remove prefix
          .map(
              name ->
                  Character.toLowerCase(name.charAt(0))
                      + name.substring(1)) // Convert to field name
          .filter(getterName -> !ignoredGetters.contains(getterName)) // Skip ignored getters
          .collect(Collectors.toSet());
    }
  }
}
