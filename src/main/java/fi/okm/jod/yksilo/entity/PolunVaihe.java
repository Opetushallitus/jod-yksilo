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
import fi.okm.jod.yksilo.domain.PolunVaiheTyyppi;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Basic;
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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@Table(indexes = {@Index(columnList = "polun_suunnitelma_id")})
public class PolunVaihe {
  @GeneratedValue @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(updatable = false, nullable = false)
  private PolunSuunnitelma polunSuunnitelma;

  @Setter
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private PolunVaiheTyyppi tyyppi;

  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 100)
  private Map<Kieli, Kaannos> kaannos;

  @ElementCollection private Set<String> linkit = new HashSet<>();

  @Setter private LocalDate alkuPvm;
  @Setter private LocalDate loppuPvm;

  @ManyToMany
  @BatchSize(size = 100)
  private Set<Osaaminen> osaamiset = new HashSet<>();

  @Setter
  @Column(nullable = false)
  @Schema(defaultValue = "false")
  private boolean valmis = false;

  protected PolunVaihe() {
    // For JPA
  }

  public PolunVaihe(PolunSuunnitelma polunSuunnitelma) {
    this.polunSuunnitelma = requireNonNull(polunSuunnitelma);
    this.kaannos = new EnumMap<>(Kieli.class);
    this.osaamiset = new HashSet<>();
  }

  public LocalizedString getNimi() {
    return LocalizedString.of(kaannos, Kaannos::getNimi);
  }

  public void setNimi(LocalizedString nimi) {
    merge(nimi, kaannos, Kaannos::new, Kaannos::setNimi);
  }

  public LocalizedString getKuvaus() {
    return LocalizedString.of(kaannos, Kaannos::getKuvaus);
  }

  public void setKuvaus(LocalizedString kuvaus) {
    merge(kuvaus, kaannos, Kaannos::new, Kaannos::setKuvaus);
  }

  public void setLinkit(Collection<String> entities) {
    linkit.clear();
    linkit.addAll(entities);
  }

  public void setOsaamiset(Collection<Osaaminen> entities) {
    osaamiset.clear();
    osaamiset.addAll(entities);
  }

  @Embeddable
  @Data
  static class Kaannos implements Translation {
    @Basic(optional = false)
    String nimi;

    @Column(length = Integer.MAX_VALUE)
    String kuvaus;

    public boolean isEmpty() {
      return nimi == null;
    }
  }
}
