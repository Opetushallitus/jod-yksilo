/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
public class Tyopaikka {
  @GeneratedValue @Id private UUID id;

  @Setter private LocalDate alkuPvm;
  @Setter private LocalDate loppuPvm;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(updatable = false, nullable = false)
  private Yksilo yksilo;

  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 20)
  @NotEmpty
  private Map<Kieli, Kaannos> kaannos;

  @Getter
  @OneToMany(mappedBy = "tyopaikka", fetch = FetchType.LAZY)
  @BatchSize(size = 20)
  private List<Toimenkuva> toimenkuvat = new ArrayList<>();

  protected Tyopaikka() {
    // For JPA
  }

  public Tyopaikka(Yksilo yksilo, LocalizedString nimi) {
    this.yksilo = yksilo;
    var tmp = new EnumMap<Kieli, Kaannos>(Kieli.class);
    nimi.asMap().forEach((key, value) -> tmp.put(key, new Kaannos(value)));
    this.kaannos = tmp;
  }

  public LocalizedString getNimi() {
    return new LocalizedString(kaannos, Kaannos::nimi);
  }

  public void setNimi(LocalizedString nimi) {
    kaannos.keySet().retainAll(nimi.asMap().keySet());
    nimi.asMap().forEach((key, value) -> kaannos.put(key, new Kaannos(value)));
  }

  @Embeddable
  public record Kaannos(@Basic(optional = false) String nimi) {}
}
