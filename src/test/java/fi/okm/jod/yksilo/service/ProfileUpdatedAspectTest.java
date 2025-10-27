/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fi.okm.jod.yksilo.IntegrationTest;
import fi.okm.jod.yksilo.domain.JodUser;
import fi.okm.jod.yksilo.entity.Yksilo;
import fi.okm.jod.yksilo.repository.YksiloRepository;
import fi.okm.jod.yksilo.service.profiili.ProfileUpdatedAspect;
import fi.okm.jod.yksilo.service.profiili.YksiloService;
import fi.okm.jod.yksilo.testutil.TestJodUser;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Isolated // Because of logging configuration changes
public class ProfileUpdatedAspectTest extends IntegrationTest {
  @Autowired private YksiloService yksiloService;
  @Autowired private YksiloRepository yksilot;
  private MockAppender mockAppender;
  private JodUser jodUser;

  @BeforeEach
  void before() {
    var logger = (Logger) LoggerFactory.getLogger(ProfileUpdatedAspect.class);
    var appender = new MockAppender();
    appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
    appender.start();
    logger.addAppender(appender);
    mockAppender = appender;

    var id = yksilot.findIdByHenkiloId("TEST:" + UUID.randomUUID());
    var yksilo = new Yksilo(id);
    yksilot.save(yksilo);
    jodUser = new TestJodUser(id);
  }

  @AfterEach
  void after() {
    var appender = mockAppender;
    mockAppender = null;
    if (appender != null) {
      ((Logger) LoggerFactory.getLogger(ProfileUpdatedAspect.class)).detachAppender(appender);
      appender.stop();
    }
  }

  @Test
  void shouldLogProfileUpdate() {
    yksiloService.get(jodUser);
    assertFalse(mockAppender.anyMatch("profile updated"));

    yksiloService.delete(jodUser);
    assertTrue(mockAppender.anyMatch("profile updated"));
  }

  static class MockAppender extends ListAppender<ILoggingEvent> {
    public boolean anyMatch(String message) {
      return list.stream()
          .map(ILoggingEvent::getFormattedMessage)
          .anyMatch(msg -> msg.contains(message));
    }

    public void clear() {
      list.clear();
    }
  }
}
