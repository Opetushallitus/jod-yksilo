/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.koski;

import com.fasterxml.jackson.annotation.JsonInclude;
import fi.okm.jod.yksilo.domain.LocalizedString;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Asks the education extraction service to identify the skills of the educations, each identified
 * by its id in the import task's result.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
record KoskiExtractionRequest(
    UUID taskId, UUID userId, List<Koulutuskokonaisuus> koulutuskokonaisuudet) {

  /**
   * One study right. {@code oppilaitos} identifies the institution, also where its records localize
   * the name differently; {@code oppilaitostyyppi} tells the level of the studies.
   */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  record Koulutuskokonaisuus(
      String oppilaitos,
      LocalizedString nimi,
      LocalizedString oppilaitostyyppi,
      LocalizedString koulutusmuoto,
      LocalDate alkuPvm,
      LocalDate loppuPvm,
      LocalizedString tila,
      List<Koulutus> koulutukset) {}

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  record Koulutus(
      UUID id,
      LocalizedString nimi,
      LocalizedString tyyppi,
      LocalizedString koulutusohjelma,
      LocalizedString kuvaus,
      List<LocalizedString> tutkintonimikkeet,
      List<LocalizedString> osaamisalat,
      List<Opinto> opinnot) {}

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  record Opinto(
      LocalizedString nimi,
      LocalizedString kieli,
      Laajuus laajuus,
      @JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean kesken,
      LocalizedString kuvaus,
      List<Opinto> opinnot) {}

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  record Laajuus(Number arvo, LocalizedString yksikko) {}
}
