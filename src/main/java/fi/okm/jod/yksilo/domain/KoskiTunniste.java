/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Identifies the Koski opiskeluoikeus an imported item originates from.
 *
 * <p>Koski has no single identifier for all opiskeluoikeus types. Those stored in Koski have an
 * {@code oid}, which alone identifies them, so no other field may be set with it. Others (e.g. from
 * Virta) are identified only by the id in the source system, which is unique only within the source
 * system type and the organisation, so all the other fields are set to make it identifying.
 *
 * <p>The whole value is the identity, so it is a flat set of identifying fields in which
 * unavailable fields are null. It is stored in the columns of the owning table, and a value with no
 * fields is stored as, and read back as, null.
 *
 * @param oid Koski generated opiskeluoikeus oid
 * @param lahdejarjestelma type of the source system, e.g. "virta"
 * @param lahdejarjestelmaId opiskeluoikeus id in the source system
 * @param toimijaOid oid of the organisation: the oppilaitos, or the koulutustoimija when there is
 *     no oppilaitos
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record KoskiTunniste(
    String oid, String lahdejarjestelma, String lahdejarjestelmaId, String toimijaOid) {

  /** Identifies an opiskeluoikeus stored in Koski. */
  public static KoskiTunniste ofOid(String oid) {
    return new KoskiTunniste(oid, null, null, null);
  }

  /** Identifies an opiskeluoikeus that has no oid, by its id in the source system. */
  public static KoskiTunniste ofLahdejarjestelma(
      String lahdejarjestelma, String lahdejarjestelmaId, String toimijaOid) {
    return new KoskiTunniste(null, lahdejarjestelma, lahdejarjestelmaId, toimijaOid);
  }
}
