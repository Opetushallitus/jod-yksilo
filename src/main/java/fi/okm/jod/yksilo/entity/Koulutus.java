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

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.LocalizedString;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
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
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
public class Koulutus {
  @GeneratedValue @Id private UUID id;

  @Setter private LocalDate alkuPvm;
  @Setter private LocalDate loppuPvm;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(updatable = false, nullable = false)
  private Yksilo yksilo;

  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 10)
  private Map<Kieli, Kaannos> kaannos;

  protected Koulutus() {}

  public Koulutus(Yksilo yksilo) {
    this.yksilo = yksilo;
    this.kaannos = new EnumMap<>(Kieli.class);
  }

  public LocalizedString getNimi() {
    return new LocalizedString(kaannos, Kaannos::getNimi);
  }

  public LocalizedString getKuvaus() {
    return new LocalizedString(kaannos, Kaannos::getKuvaus);
  }

  public void setNimi(LocalizedString nimi) {
    merge(nimi, kaannos, Kaannos::new, Kaannos::setNimi);
  }

  public void setKuvaus(LocalizedString kuvaus) {
    merge(kuvaus, kaannos, Kaannos::new, Kaannos::setKuvaus);
  }

  @Embeddable
  @Data
  static class Kaannos implements Translation {
    @Basic(optional = false)
    String nimi;

    @Column(columnDefinition = "TEXT")
    String kuvaus;

    public boolean isEmpty() {
      return nimi == null && kuvaus == null;
    }
  }
}
