/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import fi.okm.jod.yksilo.domain.CvTehtavaTila;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.dto.profiili.CvTehtavaDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(indexes = {@Index(columnList = "yksilo_id")})
public class CvTehtava extends JodEntity {

  @Id
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(updatable = false, nullable = false)
  private Yksilo yksilo;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CvTehtavaTila tila;

  @Setter
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private CvTehtavaDto.Tulos tulos;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Kieli kieli;

  protected CvTehtava() {}

  public CvTehtava(Yksilo yksilo, Kieli kieli) {
    this.id = UUID.randomUUID();
    this.yksilo = yksilo;
    this.tila = CvTehtavaTila.ODOTTAA;
    this.kieli = kieli;
  }
}
