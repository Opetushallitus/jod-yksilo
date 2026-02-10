/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.entity;

import com.google.errorprone.annotations.Immutable;
import fi.okm.jod.yksilo.domain.Kieli;
import fi.okm.jod.yksilo.domain.KoulutusmahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.MahdollisuusTyyppi;
import fi.okm.jod.yksilo.domain.TyomahdollisuusAineisto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(name = "mahdollisuus_view")
@Immutable
@Getter // Read-only view
public class MahdollisuusView {

  @Id private UUID id;

  @Column(name = "tyyppi")
  @Enumerated(EnumType.STRING)
  private MahdollisuusTyyppi tyyppi;

  @Column(name = "ammattiryhma")
  private String ammattiryhma;

  @Column(name = "aineisto")
  @Enumerated(EnumType.STRING)
  private TyomahdollisuusAineisto aineisto;

  @Column(name = "otsikko")
  private String otsikko;

  @Column(name = "koulutus_tyyppi")
  @Enumerated(EnumType.STRING)
  private KoulutusmahdollisuusTyyppi koulutusTyyppi;

  @Column(name = "maakunnat")
  private List<String> maakunnat;

  @Column(name = "toimialat")
  private List<String> toimialat;

  @Column(name = "kesto")
  private Double kesto;

  @Column private Double kestoMaksimi;

  @Column private Double kestoMinimi;

  @Column(name = "kieli")
  @Enumerated(EnumType.STRING)
  private Kieli kieli;
}
