/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity.koulutusmahdollisuus;

import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusJakaumaTyyppi;
import fi.okm.jod.yksilo.entity.Jakauma;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Getter
@Table(indexes = {@Index(columnList = "koulutusmahdollisuus_id, tyyppi", unique = true)})
public class KoulutusmahdollisuusJakauma implements Jakauma<KoulutusmahdollisuusJakaumaTyyppi> {
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Id
  private long id;

  @ManyToOne(fetch = FetchType.LAZY)
  private Koulutusmahdollisuus koulutusmahdollisuus;

  @Enumerated(EnumType.STRING)
  private KoulutusmahdollisuusJakaumaTyyppi tyyppi;

  private int maara;
  private int tyhjia;

  @ElementCollection
  @BatchSize(size = 100)
  @CollectionTable(indexes = {@Index(columnList = "koulutusmahdollisuus_jakauma_id")})
  private List<Jakauma.Arvo> arvot;
}
