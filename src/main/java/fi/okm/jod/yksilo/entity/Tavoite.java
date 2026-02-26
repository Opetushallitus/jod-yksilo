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
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.TyomahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.entity.tyomahdollisuus.Tyomahdollisuus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@Table(indexes = {@Index(columnList = "yksilo_id")})
public class Tavoite {
  @GeneratedValue @Id private UUID id;

  @Column(nullable = false, updatable = false)
  private Instant luotu = Instant.now();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(updatable = false, nullable = false)
  private Yksilo yksilo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @Setter
  private Tyomahdollisuus tyomahdollisuus;

  @OneToMany(
      mappedBy = "tavoite",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @BatchSize(size = 100)
  private List<PolunSuunnitelma> suunnitelmat = new ArrayList<>();

  @Getter(AccessLevel.NONE)
  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 100)
  private Map<Kieli, Kaannos> kaannos = new EnumMap<>(Kieli.class);

  protected Tavoite() {
    // For JPA
  }

  public Tavoite(
      Yksilo yksilo,
      Tyomahdollisuus tyomahdollisuus,
      LocalizedString tavoite,
      LocalizedString kuvaus) {
    this.yksilo = yksilo;
    this.tyomahdollisuus = tyomahdollisuus;

    merge(tavoite, kaannos, Kaannos::new, Kaannos::setTavoite);
    merge(kuvaus, kaannos, Kaannos::new, Kaannos::setKuvaus);
  }

  public Tavoite(final Yksilo yksilo, final LocalizedString tavoite, final LocalizedString kuvaus) {
    this.yksilo = yksilo;

    merge(tavoite, kaannos, Kaannos::new, Kaannos::setTavoite);
    merge(kuvaus, kaannos, Kaannos::new, Kaannos::setKuvaus);
  }

  public LocalizedString getTavoite() {
    return LocalizedString.of(kaannos, Kaannos::getTavoite);
  }

  public void setTavoite(LocalizedString tavoite) {
    merge(tavoite, kaannos, Kaannos::new, Kaannos::setTavoite);
  }

  public void setKuvaus(LocalizedString kuvaus) {
    merge(kuvaus, kaannos, Kaannos::new, Kaannos::setKuvaus);
  }

  public MahdollisuusTyyppi getMahdollisuusTyyppi() {
    return tyomahdollisuus != null
        ? MahdollisuusTyyppi.TYOMAHDOLLISUUS
        : MahdollisuusTyyppi.KOULUTUSMAHDOLLISUUS;
  }

  public UUID getMahdollisuusId() {
    if (tyomahdollisuus != null) {
      return tyomahdollisuus.getId();
    }
    return null;
  }

  public Set<URI> getOsaamiset() {
    var jakauma = tyomahdollisuus.getJakaumat().get(TyomahdollisuusJakaumaTyyppi.OSAAMINEN);
    if (jakauma != null && jakauma.getArvot() != null) {
      return jakauma.getArvot().stream()
          .map(Jakauma.Arvo::arvo)
          .map(URI::create)
          .collect(Collectors.toUnmodifiableSet());
    }

    return emptySet();
  }

  public LocalizedString getKuvaus() {
    return LocalizedString.of(kaannos, Kaannos::getKuvaus);
  }

  @Embeddable
  @Data
  static class Kaannos implements Translation {
    @Column(length = Integer.MAX_VALUE)
    String tavoite;

    @Column(length = Integer.MAX_VALUE)
    String kuvaus;

    public boolean isEmpty() {
      return tavoite == null && kuvaus == null;
    }
  }
}
