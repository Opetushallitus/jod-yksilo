/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.tmt;

import static org.junit.jupiter.api.Assertions.*;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

class TmtAuthorizationRepositoryTest {

  private final TmtConfiguration config =
      new TmtConfiguration(
          true,
          URI.create("http://auth.local"),
          URI.create("http://api.local"),
          "apikey",
          "issuer");

  static class TestClock extends Clock {
    Instant instant = Instant.now();

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
      return instant;
    }

    public void setInstant(Instant instant) {
      this.instant = instant;
    }
  }

  @Test
  void shouldExpireRequest() {
    var clock = new TestClock();
    var session = new MockHttpSession();
    var repository = new TmtAuthorizationRepository(session, config, clock);
    var request = repository.createAuthorizationRequest(URI.create("http://redirect.local"));
    assertNotNull(repository.getAuthorizationRequest(request.id()));
    clock.setInstant(clock.instant().plusSeconds(300));
    assertNull(repository.getAuthorizationRequest(request.id()));
  }

  @Test
  void shouldRejectInvalidAccessToken() {
    var session = new MockHttpSession();
    var repository = new TmtAuthorizationRepository(session, config);
    var request = repository.createAuthorizationRequest(URI.create("http://redirect.local"));

    assertThrows(InvalidTokenException.class, () -> repository.saveAccessToken(request, "token"));
  }

  @Test
  void shouldRejectOutdatedAccessToken() {
    var session = new MockHttpSession();
    var repository = new TmtAuthorizationRepository(session, config);
    var request = repository.createAuthorizationRequest(URI.create("http://redirect.local"));

    var issued = Instant.now().minusSeconds(1800);
    var token =
        new PlainJWT(
                new JWTClaimsSet.Builder()
                    .issuer(config.getTokenIssuer())
                    .issueTime(Date.from(issued))
                    .expirationTime(Date.from(issued.plusSeconds(3600)))
                    .build())
            .serialize();

    assertThrows(InvalidTokenException.class, () -> repository.saveAccessToken(request, token));
  }

  @Test
  void shouldRejectExpiredAccessToken() {
    var session = new MockHttpSession();
    var repository = new TmtAuthorizationRepository(session, config);
    var request = repository.createAuthorizationRequest(URI.create("http://redirect.local"));

    var token =
        new PlainJWT(
                new JWTClaimsSet.Builder()
                    .issuer(config.getTokenIssuer())
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().minusSeconds(3600)))
                    .build())
            .serialize();

    assertThrows(InvalidTokenException.class, () -> repository.saveAccessToken(request, token));
  }

  @Test
  void shouldAcceptAndSaveValidToken() {
    var session = new MockHttpSession();
    var repository = new TmtAuthorizationRepository(session, config);
    var request = repository.createAuthorizationRequest(URI.create("http://redirect.local"));
    assertEquals(request, repository.getAuthorizationRequest(request.id()));

    var token =
        new PlainJWT(
                new JWTClaimsSet.Builder()
                    .issuer(config.getTokenIssuer())
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                    .build())
            .serialize();

    assertDoesNotThrow(() -> repository.saveAccessToken(request, token));
    var accessToken = repository.getAccessToken(request.id());
    assertEquals(token, accessToken.token());
  }
}
