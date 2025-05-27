/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.tmt;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.net.URI;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jod.tmt.enabled", havingValue = "true")
public class TmtAuthorizationRepository {
  private static final String REQUEST_ATTRIBUTE =
      TmtAuthorizationRepository.class.getName() + ".REQUEST";
  private static final String TOKEN_ATTRIBUTE =
      TmtAuthorizationRepository.class.getName() + ".TOKEN";

  private final HttpSession session;
  private final TmtConfiguration config;
  private final Clock clock;

  @Autowired
  public TmtAuthorizationRepository(HttpSession session, TmtConfiguration config) {
    this(session, config, Clock.systemUTC());
  }

  TmtAuthorizationRepository(HttpSession session, TmtConfiguration config, Clock clock) {
    this.session = session;
    this.config = config;
    this.clock = clock;
  }

  public AuthorizationRequest createAuthorizationRequest(URI callback) {
    var request =
        new AuthorizationRequest(
            UUID.randomUUID(), Instant.now(clock).plusSeconds(300), callback.normalize().getPath());
    session.setAttribute(REQUEST_ATTRIBUTE, request);
    return request;
  }

  public AuthorizationRequest getAuthorizationRequest(UUID requestId) {
    if (session.getAttribute(REQUEST_ATTRIBUTE) instanceof AuthorizationRequest request
        && request.id().equals(requestId)) {
      if (request.expires().isAfter(Instant.now(clock))) {
        return request;
      } else {
        clearAuthorizationRequest();
        return null;
      }
    }
    return null;
  }

  public void clearAuthorizationRequest() {
    session.removeAttribute(REQUEST_ATTRIBUTE);
  }

  public void saveAccessToken(AuthorizationRequest request, String token) {
    Objects.requireNonNull(request);
    Objects.requireNonNull(token);
    try {
      var jwt = JWTParser.parse(token);
      // note. we are currently unable to verify the signature (no public key available)
      var claims = jwt.getJWTClaimsSet();
      validateClaims(claims);
      var accessToken =
          new AccessToken(request.id(), token, claims.getExpirationTime().toInstant());
      session.setAttribute(TOKEN_ATTRIBUTE, accessToken);
    } catch (ParseException e) {
      throw new InvalidTokenException("Invalid access token", e);
    }
  }

  private void validateClaims(JWTClaimsSet claims) {
    if (claims == null
        || claims.getIssuer() == null
        || claims.getExpirationTime() == null
        || claims.getIssueTime() == null) {
      throw new InvalidTokenException("Invalid access token");
    }
    if (!config.getTokenIssuer().equals(claims.getIssuer())) {
      throw new InvalidTokenException("Invalid issuer");
    }
    final var now = Instant.now(clock);
    var issued = claims.getIssueTime().toInstant();
    if (issued.isAfter(now.plusSeconds(60))) {
      throw new InvalidTokenException("Access token is not yet valid");
    }
    if (issued.isBefore(now.minusSeconds(300))) {
      throw new InvalidTokenException("Access token is not fresh");
    }
    if (claims.getExpirationTime().toInstant().isBefore(now)) {
      throw new InvalidTokenException("Access token has expired");
    }
  }

  public AccessToken getAccessToken(UUID id) {
    if (session.getAttribute(TOKEN_ATTRIBUTE) instanceof AccessToken token
        && token.id().equals(id)) {
      if (token.expires().isAfter(Instant.now(clock))) {
        return token;
      } else {
        // Token expired
        clearAccessToken();
        return null;
      }
    }
    return null;
  }

  public void clearAccessToken() {
    session.removeAttribute(TOKEN_ATTRIBUTE);
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
  public record AuthorizationRequest(UUID id, Instant expires, String callback)
      implements Serializable {}

  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
  public record AccessToken(UUID id, String token, Instant expires) implements Serializable {}
}
