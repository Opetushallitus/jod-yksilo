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
import static java.util.Objects.requireNonNull;

import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.KoskiTunniste;
import fi.okm.jod.yksilo.domain.LocalizedString;
import fi.okm.jod.yksilo.domain.TuontiLahde;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
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
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SecondaryRow;

@Entity
@Table(indexes = {@Index(columnList = "yksilo_id")})
@SecondaryTable(
    name = KoulutusKokonaisuus.KOSKI_TUNNISTE,
    pkJoinColumns = @PrimaryKeyJoinColumn(name = "koulutus_kokonaisuus_id"))
@SecondaryRow(table = KoulutusKokonaisuus.KOSKI_TUNNISTE, optional = true)
public class KoulutusKokonaisuus {
  static final String KOSKI_TUNNISTE = "koulutus_kokonaisuus_koski_tunniste";

  @Id @GeneratedValue @Getter UUID id;

  @Setter
  @Getter
  @Enumerated(EnumType.STRING)
  private TuontiLahde tuontiLahde;

  @Setter
  @Getter
  @Embedded
  @AttributeOverride(name = "oid", column = @Column(table = KOSKI_TUNNISTE))
  @AttributeOverride(name = "lahdejarjestelma", column = @Column(table = KOSKI_TUNNISTE))
  @AttributeOverride(name = "lahdejarjestelmaId", column = @Column(table = KOSKI_TUNNISTE))
  @AttributeOverride(name = "toimijaOid", column = @Column(table = KOSKI_TUNNISTE))
  private KoskiTunniste koskiTunniste;

  @Getter
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(updatable = false, nullable = false)
  private Yksilo yksilo;

  @ElementCollection
  @MapKeyEnumerated(EnumType.STRING)
  @BatchSize(size = 100)
  private Map<Kieli, Kaannos> kaannos;

  @OneToMany(
      mappedBy = "kokonaisuus",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @BatchSize(size = 100)
  @Getter
  private List<Koulutus> koulutukset = new ArrayList<>();

  protected KoulutusKokonaisuus() {
    // For JPA
  }

  public KoulutusKokonaisuus(Yksilo yksilo, LocalizedString nimi) {
    this.yksilo = requireNonNull(yksilo);
    this.kaannos = new EnumMap<>(Kieli.class);
    merge(nimi, kaannos, Kaannos::new, Kaannos::setNimi);
  }

  public LocalizedString getNimi() {
    return LocalizedString.of(kaannos, Kaannos::getNimi);
  }

  public void setNimi(LocalizedString nimi) {
    merge(nimi, kaannos, Kaannos::new, Kaannos::setNimi);
  }

  @Embeddable
  @Data
  static class Kaannos implements Translation {
    @NotNull
    @Basic(optional = false)
    String nimi;

    public boolean isEmpty() {
      return nimi == null;
    }
  }
}
