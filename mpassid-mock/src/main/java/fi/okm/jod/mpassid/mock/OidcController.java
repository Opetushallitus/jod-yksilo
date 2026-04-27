/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.mpassid.mock;

import static fi.okm.jod.mpassid.mock.OnrUtils.onr;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oidc")
@Slf4j
public class OidcController {

  /** The OIDC claim used by MPASSid to carry the oppijanumero. */
  private static final String OPPIJANUMERO_CLAIM = "urn:oid:1.3.6.1.4.1.16161.1.1.27";

  private static final List<TestUser> TEST_USERS =
      List.of(
          new TestUser("Matti", "Meikäläinen", onr(1000000000L)),
          new TestUser("Maija", "Meikäläinen", onr(2000000000L)),
          new TestUser("Teppo", "Testaaja", onr(3000000000L)),
          new TestUser("Nils", "Haapakosi", onr(9269043271L)),
          new TestUser("User", "Invalid", "1.2.3.4.5.40000000007"));

  private final RSAKey rsaKey;
  private final RSASSASigner signer;
  private final String issuer;

  private final ConcurrentHashMap<String, AuthCode> authCodes = new ConcurrentHashMap<>();

  public OidcController(@Value("${mock.issuer}") String issuer) throws JOSEException {
    this.issuer = issuer;
    this.rsaKey = new RSAKeyGenerator(2048).keyID("mpassid-mock-key").generate();
    this.signer = new RSASSASigner(rsaKey);
    log.info("MPASSid mock OIDC provider started, issuer={}", issuer);
  }

  @GetMapping(
      value = "/.well-known/openid-configuration",
      produces = MediaType.APPLICATION_JSON_VALUE)
  Map<String, Object> discovery() {
    return Map.of(
        "issuer",
        issuer,
        "authorization_endpoint",
        issuer + "/authorize",
        "token_endpoint",
        issuer + "/token",
        "jwks_uri",
        issuer + "/jwks",
        "response_types_supported",
        List.of("code"),
        "subject_types_supported",
        List.of("public"),
        "id_token_signing_alg_values_supported",
        List.of("RS256"));
  }

  @GetMapping(value = "/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
  String jwks() {
    return new JWKSet(rsaKey.toPublicJWK()).toString();
  }

  @GetMapping(value = "/authorize", produces = MediaType.TEXT_HTML_VALUE)
  String authorize(
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam(value = "state", required = false) String state,
      @RequestParam(value = "nonce", required = false) String nonce,
      @RequestParam(value = "client_id", required = false) String clientId) {

    var sb = new StringBuilder();
    var leadText = "Choose a pre-defined test user or enter a custom profile.";
    if (clientId != null && !clientId.isBlank()) {
      leadText += " Client: " + clientId;
    }

    sb.append(
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>MPASSid Mock Login</title>
        <style>
          :root {
            color-scheme: light;
            --page-bg: #f3f6fb;
            --panel-bg: #ffffff;
            --panel-border: #cbd5e1;
            --text-strong: #0f172a;
            --text-body: #1e293b;
            --text-muted: #475569;
            --accent: #0f4c81;
            --accent-hover: #0b3a63;
            --accent-soft: #e8f1fb;
            --field-border: #94a3b8;
            --field-bg: #ffffff;
          }

          * { box-sizing: border-box; }

          body {
            margin: 0;
            padding: 16px 16px;
            background: var(--page-bg);
            color: var(--text-body);
            font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont,
                "Segoe UI", sans-serif;
            line-height: 1.2;
          }

          .page {
            max-width: 640px;
            margin: 0 auto;
          }

          .hero {
            margin-bottom: 12px;
          }

          h1 {
            margin: 0 0 4px;
            color: var(--text-strong);
            font-size: 1.5rem;
            line-height: 1.1;
          }

          .lead {
            margin: 0;
            color: var(--text-muted);
            font-size: 1rem;
          }

          .panel {
            background: var(--panel-bg);
            border: 1px solid var(--panel-border);
            border-radius: 12px;
            padding: 16px;
            box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
          }

          .section + .section {
            margin-top: 16px;
          }

          .section-title {
            margin: 0 0 8px;
            color: var(--text-strong);
            font-size: 1rem;
            font-weight: 700;
          }

          .user-list {
            display: grid;
            gap: 6px;
          }

          .user-form {
            margin: 0;
          }

          button.user {
            display: block;
            width: 100%%;
            margin: 0;
            padding: 8px 12px;
            background: #ffffff;
            border: 1px solid var(--panel-border);
            border-radius: 8px;
            color: var(--text-strong);
            text-align: left;
            cursor: pointer;
            transition: background-color 120ms ease, border-color 120ms ease,
                box-shadow 120ms ease;
          }

          button.user:hover {
            background: var(--accent-soft);
            border-color: var(--accent);
            box-shadow: 0 2px 8px rgba(15, 76, 129, 0.12);
          }

          button.user:focus-visible,
          .primary-button:focus-visible,
          input[type=text]:focus-visible {
            outline: 3px solid #1d70b8;
            outline-offset: 2px;
          }

          .user-name {
            color: var(--text-strong);
            font-size: 0.95rem;
            font-weight: 700;
          }

          .oid {
            margin-left: 8px;
            color: var(--text-muted);
            font-size: 0.85rem;
            font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas,
                "Liberation Mono", monospace;
          }

          fieldset {
            margin: 0;
            padding: 0;
            border: 0;
          }

          legend {
            padding: 0;
            margin-bottom: 6px;
            color: var(--text-strong);
            font-size: 1rem;
            font-weight: 700;
          }

          .helper {
            margin: 0 0 10px;
            color: var(--text-muted);
            font-size: 0.85rem;
          }

          .fields-row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 10px;
          }

          .field-full {
            margin-top: 10px;
          }

          label {
            display: block;
            margin-bottom: 3px;
            color: var(--text-strong);
            font-weight: 600;
            font-size: 0.9rem;
          }

          input[type=text] {
            width: 100%%;
            padding: 8px 10px;
            border: 1px solid var(--field-border);
            border-radius: 8px;
            background: var(--field-bg);
            color: var(--text-strong);
            font-size: 0.95rem;
          }

          input[type=text]::placeholder {
            color: var(--text-muted);
            opacity: 1;
          }

          .actions {
            margin-top: 12px;
          }

          .primary-button {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            min-width: 100px;
            padding: 8px 16px;
            border: 0;
            border-radius: 8px;
            background: var(--accent);
            color: #ffffff;
            font-size: 0.95rem;
            font-weight: 700;
            cursor: pointer;
            transition: background-color 120ms ease;
          }

          .primary-button:hover {
            background: var(--accent-hover);
          }
        </style>
        </head><body>
        <main class="page">
          <header class="hero">
            <h1>MPASSid Mock Login</h1>
            <p class="lead">%s</p>
          </header>
          <section class="panel">
            <div class="section">
              <h2 class="section-title">Test users</h2>
              <div class="user-list">
        """
            .formatted(leadText));

    var formAction = issuer + "/authorize/login";

    for (var user : TEST_USERS) {
      sb.append(
          String.format(
              """
              <form class="user-form" method="post" action="%s">
                <input type="hidden" name="redirect_uri" value="%s">
                <input type="hidden" name="state" value="%s">
                <input type="hidden" name="nonce" value="%s">
                <input type="hidden" name="given_name" value="%s">
                <input type="hidden" name="family_name" value="%s">
                <input type="hidden" name="oppijanumero" value="%s">
                <button class="user" type="submit">
                  <span class="user-name">%s %s</span>
                  <span class="oid">%s</span>
                </button>
              </form>
              """,
              formAction,
              redirectUri,
              state != null ? state : "",
              nonce != null ? nonce : "",
              user.givenName(),
              user.familyName(),
              user.oppijanumero(),
              user.givenName(),
              user.familyName(),
              user.oppijanumero()));
    }

    sb.append(
        String.format(
            """
              </div>
            </div>
            <div class="section">
            <form method="post" action="%s">
              <fieldset>
                <legend>Custom User</legend>
                <p class="helper">Use this to test users not included in the quick list above.</p>
                <input type="hidden" name="redirect_uri" value="%s">
                <input type="hidden" name="state" value="%s">
                <input type="hidden" name="nonce" value="%s">
                <div class="fields-row">
                  <div class="field-full">
                    <label for="given_name">Given name</label>
                    <input id="given_name" type="text" name="given_name" required>
                  </div>
                  <div class="field-full">
                    <label for="family_name">Family name</label>
                    <input id="family_name" type="text" name="family_name" required>
                  </div>
                </div>
                <div class="field-full">
                  <label for="oppijanumero">Oppijanumero</label>
                  <input id="oppijanumero" type="text" name="oppijanumero" required placeholder="1.2.246.562.24.x">
                </div>
                <div class="actions">
                  <button class="primary-button" type="submit">Log in</button>
                </div>
              </fieldset>
            </form>
            </div>
          </section>
        </main>
        </body></html>
        """,
            formAction, redirectUri, state != null ? state : "", nonce != null ? nonce : ""));
    return sb.toString();
  }

  @PostMapping(value = "/authorize/login")
  ResponseEntity<Void> login(
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam(value = "state", required = false) String state,
      @RequestParam(value = "nonce", required = false) String nonce,
      @RequestParam("given_name") String givenName,
      @RequestParam("family_name") String familyName,
      @RequestParam("oppijanumero") String oppijanumero) {

    var code = UUID.randomUUID().toString();
    var user = new TestUser(givenName, familyName, oppijanumero);
    authCodes.put(code, new AuthCode(user, nonce));

    var location = redirectUri + "?code=" + code;
    if (state != null && !state.isEmpty()) {
      location += "&state=" + state;
    }

    return ResponseEntity.status(302).header("Location", location).build();
  }

  @PostMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<Map<String, Object>> token(
      @RequestParam("code") String code,
      @RequestParam(value = "grant_type") String grantType,
      @RequestParam(value = "redirect_uri") String redirectUri)
      throws JOSEException {

    if (!"authorization_code".equals(grantType)) {
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  "error",
                  "unsupported_grant_type",
                  "error_description",
                  "Only authorization_code grant type is supported"));
    }

    if (redirectUri.isBlank()) {
      return ResponseEntity.badRequest()
          .body(
              Map.of("error", "invalid_request", "error_description", "redirect_uri is required"));
    }

    var authCode = authCodes.remove(code);
    if (authCode == null) {
      return ResponseEntity.badRequest()
          .body(Map.of("error", "invalid_grant", "error_description", "Invalid or expired code"));
    }

    var user = authCode.user();

    var now = Instant.now();
    var claimsBuilder =
        new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(user.oppijanumero())
            .audience(List.of("mpassid-client"))
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(3600)))
            .claim("given_name", user.givenName())
            .claim("family_name", user.familyName())
            .claim(OPPIJANUMERO_CLAIM, user.oppijanumero());

    if (authCode.nonce() != null) {
      claimsBuilder.claim("nonce", authCode.nonce());
    }

    var claims = claimsBuilder.build();

    var jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(), claims);
    jwt.sign(signer);

    log.info(
        "Issued token for {} {} ({})", user.givenName(), user.familyName(), user.oppijanumero());

    return ResponseEntity.ok(
        Map.of(
            "access_token",
            "mock-access-token",
            "token_type",
            "Bearer",
            "expires_in",
            3600,
            "id_token",
            jwt.serialize()));
  }

  record AuthCode(TestUser user, String nonce) {}

  record TestUser(String givenName, String familyName, String oppijanumero) {}
}
