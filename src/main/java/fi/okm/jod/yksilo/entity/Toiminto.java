/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import static java.util.Objects.requireNonNull;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@Table(indexes = {@Index(columnList = "yksilo_id")})
public class Toiminto {
  @GeneratedValue @Id private UUID id;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(updatable = false, nullable = false)
  private Yksilo yksilo;

  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 20)
  @NotEmpty
  private Map<Kieli, Kaannos> kaannos;

  @OneToMany(mappedBy = "toiminto", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
  @BatchSize(size = 20)
  private List<Patevyys> patevyydet = new ArrayList<>();

  protected Toiminto() {
    // For JPA
  }

  public Toiminto(Yksilo yksilo, LocalizedString nimi) {
    this.yksilo = requireNonNull(yksilo);
    var tmp = new EnumMap<Kieli, Kaannos>(Kieli.class);
    nimi.asMap().forEach((key, value) -> tmp.put(key, new Kaannos(value)));
    this.kaannos = tmp;
  }

  public LocalizedString getNimi() {
    return LocalizedString.of(kaannos, Kaannos::nimi);
  }

  public void setNimi(LocalizedString nimi) {
    kaannos.keySet().retainAll(nimi.asMap().keySet());
    nimi.asMap().forEach((key, value) -> kaannos.put(key, new Kaannos(value)));
  }

  @Embeddable
  public record Kaannos(@Basic(optional = false) String nimi) {}
}
