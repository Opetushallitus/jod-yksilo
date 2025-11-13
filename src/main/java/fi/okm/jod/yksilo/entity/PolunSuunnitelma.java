/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import static fi.okm.jod.yksilo.entity.Translation.merge;
import static java.util.Objects.requireNonNull;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@Table(indexes = {@Index(columnList = "tavoite_id")})
public class PolunSuunnitelma {
  @GeneratedValue @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(updatable = false, nullable = false)
  private Tavoite tavoite;

  @Getter(AccessLevel.NONE)
  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 100)
  private Map<Kieli, Kaannos> kaannos;

  @OneToMany(
      mappedBy = "polunSuunnitelma",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @BatchSize(size = 100)
  private List<PolunVaihe> vaiheet = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @Setter
  private Koulutusmahdollisuus koulutusmahdollisuus;

  @ManyToMany
  @BatchSize(size = 100)
  private Set<Osaaminen> osaamiset = new HashSet<>();

  @ManyToMany
  @BatchSize(size = 100)
  private Set<Osaaminen> ignoredOsaamiset = new HashSet<>();

  protected PolunSuunnitelma() {
    // For JPA
  }

  public PolunSuunnitelma(Tavoite tavoite) {
    this.tavoite = requireNonNull(tavoite);
    this.kaannos = new EnumMap<>(Kieli.class);
  }

  public void setOsaamiset(Collection<Osaaminen> entities) {
    osaamiset.clear();
    osaamiset.addAll(entities);
  }

  public void setIgnoredOsaamiset(Collection<Osaaminen> entities) {
    ignoredOsaamiset.clear();
    ignoredOsaamiset.addAll(entities);
  }

  public LocalizedString getNimi() {
    return LocalizedString.of(kaannos, Kaannos::getNimi);
  }

  public void setNimi(LocalizedString nimi) {
    merge(nimi, kaannos, Kaannos::new, Kaannos::setNimi);
  }

  public UUID getKoulutusmahdollisuusId() {
    if (this.getKoulutusmahdollisuus() == null) {
      return null;
    }
    return this.koulutusmahdollisuus.getId();
  }

  @Embeddable
  @Data
  static class Kaannos implements Translation {
    @Basic(optional = false)
    String nimi;

    public boolean isEmpty() {
      return nimi == null;
    }
  }
}
