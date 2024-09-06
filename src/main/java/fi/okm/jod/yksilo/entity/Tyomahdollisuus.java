/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import fi.okm.jod.yksilo.domain.JakaumaTyyppi;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

@Entity
@Getter
@Immutable
@Table(schema = "tyomahdollisuus_data")
public class Tyomahdollisuus {
  @Id private UUID id;

  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 1000)
  @CollectionTable(schema = "tyomahdollisuus_data")
  private Map<Kieli, Kaannos> kaannos;

  @OneToMany(mappedBy = "tyomahdollisuus", fetch = FetchType.LAZY)
  @BatchSize(size = 100)
  @MapKeyEnumerated(EnumType.STRING)
  @MapKeyColumn(name = "tyyppi")
  private Map<JakaumaTyyppi, Jakauma> jakaumat;

  @Embeddable
  public record Kaannos(
      @Column(columnDefinition = "TEXT") String otsikko,
      @Column(columnDefinition = "TEXT") String tiivistelma,
      @Column(columnDefinition = "TEXT") String kuvaus) {}

  public LocalizedString getOtsikko() {
    return LocalizedString.of(kaannos, Kaannos::otsikko);
  }

  public LocalizedString getKuvaus() {
    return LocalizedString.of(kaannos, Kaannos::kuvaus);
  }

  public LocalizedString getTiivistelma() {
    return LocalizedString.of(kaannos, Kaannos::tiivistelma);
  }
}
