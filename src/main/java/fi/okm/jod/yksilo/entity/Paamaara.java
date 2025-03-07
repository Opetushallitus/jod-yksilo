/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import static fi.okm.jod.yksilo.entity.Translation.merge;
import static java.util.Collections.emptySet;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.PaamaaraTyyppi;
import fi.okm.jod.yksilo.domain.TyomahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.entity.koulutusmahdollisuus.Koulutusmahdollisuus;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Check;

@Entity
@Getter
@Table(indexes = {@Index(columnList = "yksilo_id")})
@Check(constraints = "(tyomahdollisuus_id IS NULL) != (koulutusmahdollisuus_id IS NULL)")
public class Paamaara {
  @GeneratedValue @Id private UUID id;

  @Column(nullable = false, updatable = false)
  private Instant luotu = Instant.now();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(updatable = false, nullable = false)
  private Yksilo yksilo;

  @Enumerated(EnumType.STRING)
  private PaamaaraTyyppi tyyppi;

  @ManyToOne(fetch = FetchType.LAZY)
  private Tyomahdollisuus tyomahdollisuus;

  @ManyToOne(fetch = FetchType.LAZY)
  private Koulutusmahdollisuus koulutusmahdollisuus;

  @OneToMany(
      mappedBy = "paamaara",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @BatchSize(size = 100)
  private List<PolunSuunnitelma> suunnitelmat = new ArrayList<>();

  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 100)
  private Map<Kieli, Kaannos> kaannos = new EnumMap<>(Kieli.class);

  protected Paamaara() {
    // For JPA
  }

  public Paamaara(
      Yksilo yksilo,
      PaamaaraTyyppi tyyppi,
      Tyomahdollisuus tyomahdollisuus,
      LocalizedString tavoite) {
    this.yksilo = yksilo;
    this.tyomahdollisuus = tyomahdollisuus;
    this.tyyppi = tyyppi;

    merge(tavoite, kaannos, Kaannos::new, Kaannos::setTavoite);
  }

  public Paamaara(
      Yksilo yksilo,
      PaamaaraTyyppi tyyppi,
      Koulutusmahdollisuus mahdollisuus,
      LocalizedString tavoite) {
    this.yksilo = yksilo;
    this.koulutusmahdollisuus = mahdollisuus;
    this.tyyppi = tyyppi;

    merge(tavoite, kaannos, Kaannos::new, Kaannos::setTavoite);
  }

  public LocalizedString getTavoite() {
    return LocalizedString.of(kaannos, Kaannos::getTavoite);
  }

  public void setTavoite(LocalizedString tavoite) {
    merge(tavoite, kaannos, Kaannos::new, Kaannos::setTavoite);
  }

  public void setTyyppi(@NotNull PaamaaraTyyppi tyyppi) {
    this.tyyppi = tyyppi;
  }

  public MahdollisuusTyyppi getMahdollisuusTyyppi() {
    return tyomahdollisuus != null
        ? MahdollisuusTyyppi.TYOMAHDOLLISUUS
        : MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS;
  }

  public UUID getMahdollisuusId() {
    return tyomahdollisuus != null ? tyomahdollisuus.getId() : koulutusmahdollisuus.getId();
  }

  public Set<String> getOsaamiset() {
    if (getMahdollisuusTyyppi() == MahdollisuusTyyppi.TYOMAHDOLLISUUS) {
      var jakauma = tyomahdollisuus.getJakaumat().get(TyomahdollisuusJakaumaTyyppi.OSAAMINEN);
      if (jakauma != null && jakauma.getArvot() != null) {
        return jakauma.getArvot().stream()
            .map(Jakauma.Arvo::arvo)
            .collect(Collectors.toUnmodifiableSet());
      }
    } else {
      var jakauma =
          koulutusmahdollisuus.getJakaumat().get(KoulutusmahdollisuusJakaumaTyyppi.OSAAMINEN);
      if (jakauma != null && jakauma.getArvot() != null) {
        return koulutusmahdollisuus
            .getJakaumat()
            .get(KoulutusmahdollisuusJakaumaTyyppi.OSAAMINEN)
            .getArvot()
            .stream()
            .map(Jakauma.Arvo::arvo)
            .collect(Collectors.toUnmodifiableSet());
      }
    }

    return emptySet();
  }

  @Embeddable
  @Data
  static class Kaannos implements Translation {
    @Column(length = Integer.MAX_VALUE)
    String tavoite;

    public boolean isEmpty() {
      return tavoite == null;
    }
  }
}
