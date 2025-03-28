/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.event;

import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.entity.Koulutus;
import java.util.List;

public record OsaamisetTunnistusEvent(JodUser jodUser, List<Koulutus> koulutukset) {}
