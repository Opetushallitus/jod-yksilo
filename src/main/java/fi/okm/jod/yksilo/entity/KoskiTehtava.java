/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import fi.okm.jod.yksilo.domain.KoskiTehtavaTila;
import fi.okm.jod.yksilo.dto.profiili.KoskiTehtavaDto;
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
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(indexes = {@Index(columnList = "yksilo_id")})
public class KoskiTehtava extends JodEntity {

  @Id
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(updatable = false, nullable = false)
  private Yksilo yksilo;

  @Setter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private KoskiTehtavaTila tila;

  // The extraction result arrives concurrently with the user's own changes to the task.
  @Version
  @Column(nullable = false)
  private long versio;

  @Setter
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private KoskiTehtavaDto.Tulos tulos;

  protected KoskiTehtava() {}

  public KoskiTehtava(Yksilo yksilo, KoskiTehtavaDto.Tulos tulos) {
    this(yksilo, KoskiTehtavaTila.VALMIS, tulos);
  }

  private KoskiTehtava(Yksilo yksilo, KoskiTehtavaTila tila, KoskiTehtavaDto.Tulos tulos) {
    this.id = UUID.randomUUID();
    this.yksilo = yksilo;
    this.tila = tila;
    this.tulos = tulos;
  }

  /**
   * A task waiting for the skills to be extracted, holding the educations without skills in case
   * the extraction fails.
   */
  public static KoskiTehtava waiting(Yksilo yksilo, KoskiTehtavaDto.Tulos withoutSkills) {
    return new KoskiTehtava(yksilo, KoskiTehtavaTila.ODOTTAA, withoutSkills);
  }

  /** Completes a waiting task with the educations it holds. */
  public void finish() {
    this.tila = KoskiTehtavaTila.VALMIS;
  }
}
