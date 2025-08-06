/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.controller.profiili;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fi.okm.jod.yksilo.IntegrationTest;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Integrationtests for {@link KiinnostusController} */
@AutoConfigureMockMvc
public class KiinnostusControllerIT extends IntegrationTest {

  @Autowired private YksiloRepository yksilot;
  @Autowired private MockMvc mockMvc;

  @WithUserDetails("test")
  @Sql(
      scripts = {"/data/ammatti.sql"},
      executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(
      scripts = {"/data/cleanup.sql"},
      executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  @Execution(ExecutionMode.SAME_THREAD)
  @Transactional
  @Test
  void addAmmattiKiinnostus200() throws Exception {
    Instant afterCreationBeforeUpdate = Instant.now();
    final Set<URI> kiinnostukset =
        Set.of(
            URI.create("urn:ammatti1"),
            URI.create("urn:ammatti2"),
            URI.create("urn:ammatti3"),
            URI.create("urn:osaaminen1"),
            URI.create("urn:osaaminen2"),
            URI.create("urn:osaaminen3"),
            URI.create("urn:osaaminen4"),
            URI.create("urn:osaaminen5"));

    this.mockMvc
        .perform(
            put("/api/profiili/kiinnostukset/osaamiset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.objectMapper.writeValueAsString(kiinnostukset)))
        .andExpect(status().isNoContent())
        .andReturn();

    final List<Yksilo> all = this.yksilot.findAll();
    final Yksilo yksilo = all.getFirst();
    final Set<String> ammattiKiinnostukset = yksilot.findAmmattiKiinnostukset(yksilo);
    final Set<String> osaamisKiinnostukset = yksilot.findOsaamisKiinnostukset(yksilo);
    assertEquals(ammattiKiinnostukset, Set.of("urn:ammatti1", "urn:ammatti2", "urn:ammatti3"));
    assertEquals(
        osaamisKiinnostukset,
        Set.of(
            "urn:osaaminen1",
            "urn:osaaminen2",
            "urn:osaaminen3",
            "urn:osaaminen4",
            "urn:osaaminen5"));
    Instant yksiloMuokattu = yksilo.getMuokattu();
    assertTrue(yksiloMuokattu.isAfter(afterCreationBeforeUpdate));
  }
}
