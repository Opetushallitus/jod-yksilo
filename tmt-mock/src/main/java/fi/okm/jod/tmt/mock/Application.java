/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.tmt.mock;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import fi.okm.jod.yksilo.external.tmt.model.FullProfileDtoExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.FullProfileDtoExternalPut;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

@SpringBootApplication
@RestController
@Slf4j
public class Application {

  enum Outcome {
    NORMAL,
    FAIL,
    INVALID_TOKEN,
    SLOW;
  }

  public static final String BEARER_PREFIX = "Bearer ";
  private final JsonMapper objectMapper;
  private final Map<String, Outcome> issuedTokens = new ConcurrentHashMap<>();
  private final Map<UUID, Outcome> issuedCodes = new ConcurrentHashMap<>();
  private final JWSSigner signer;
  private final JWSVerifier verifier;
  private final String basicAuth;
  private final String clientId;

  @SuppressWarnings("java:S3077")
  private volatile FullProfileDtoExternalGet profile;

  public static void main(String[] args) {
    log.info("Starting TMT Mock API");
    SpringApplication.run(Application.class, args);
  }

  Application(JsonMapper.Builder objectMapperBuilder, Environment env)
      throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
    var keyGenerator = KeyPairGenerator.getInstance("EC");
    var curve = new ECGenParameterSpec("secp256r1");
    keyGenerator.initialize(curve);
    var keyPair = keyGenerator.generateKeyPair();
    this.objectMapper =
        objectMapperBuilder
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(Include.NON_EMPTY))
            .build();
    this.signer = new ECDSASigner((ECPrivateKey) keyPair.getPrivate());
    this.verifier = new ECDSAVerifier((ECPublicKey) keyPair.getPublic());
    this.clientId = env.getProperty("mock.client-id", "dummy-client-id");
    this.basicAuth =
        Base64.getEncoder()
            .encodeToString(
                (clientId + ":" + env.getProperty("mock.client-secret", "dummy-client-secret"))
                    .getBytes());
  }

  @GetMapping("/authorize")
  ResponseEntity<String> authorize(
      @RequestParam("response_type") String responseType,
      @RequestParam("client_id") String clientId,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam("state") String state) {
    log.info("Received authorization request, redirect {}", redirectUri);

    String html =
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
           <meta charset="utf-8">
           <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>TMT Mock - Authorization</title>
            <style>'
                body { max-width: 60%; margin: auto; padding: 20px; }
                form { padding: 20px; }
                button {
                  display: block;
                  margin: 10px 0;
                  padding: 10px 20px;
                  font-size: 16px;
                  border: none;
                  border-radius: 3px;
                  cursor: pointer;
                 }
                .proceed { background: darkgreen; color: white; }
                .cancel { background: orange; color: white; }
                .fail { background: darkred; color: white; }
            '</style>
        </head>
        <body>
            <h1>TMT Mock Authorization</h1>
            <form action="/authorize" method="post">
                <input type="hidden" name="redirect_uri" value="{0}">
                <input type="hidden" name="state" value="{1}">
                <input type="hidden" name="client_id" value="{2}">
                <input type="hidden" name="response_type" value="{3}">
                <button type="submit" name="action" value="proceed" class="proceed">Proceed normally</button>
                <button type="submit" name="action" value="slow_api" class="proceed">Proceed (slow API)</button>
                <button type="submit" name="action" value="cancel" class="cancel">Cancel Authorization</button>
                <button type="submit" name="action" value="fail" class="fail">Fail Authorization</button>
                <button type="submit" name="action" value="token_fail" class="fail">Proceed (token fetch will fail)</button>
                <button type="submit" name="action" value="invalid_token" class="fail">Proceed (token will be invalid)</button>
                <button type="submit" name="action" value="api_fail" class="fail">Proceed (profile API will fail)</button>
            </form>
        </body>
        </html>
        """;

    return ResponseEntity.ok()
        .header("Content-Type", "text/html")
        .body(MessageFormat.format(html, redirectUri, state, clientId, responseType));
  }

  @PostMapping("/authorize")
  ResponseEntity<Void> authorizeSubmit(
      @RequestParam("response_type") String responseType,
      @RequestParam("client_id") String clientId,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam("state") String state,
      @RequestParam(value = "action", defaultValue = "proceed") String action) {
    log.info("Authorization action: {}, redirect {}", action, redirectUri);

    var redirectUriBuilder = UriComponentsBuilder.fromUriString(redirectUri);

    if (!"code".equals(responseType) || !this.clientId.equals(clientId)) {
      log.info("Invalid authorization request parameters");
      redirectUriBuilder.queryParam("error", "invalid_request").queryParam("state", state);
    } else {
      switch (action) {
        case "proceed" -> {
          var code = UUID.randomUUID();
          issuedCodes.put(code, Outcome.NORMAL);
          redirectUriBuilder.queryParam("code", code).queryParam("state", state);
        }
        case "token_fail" -> {
          var code = UUID.randomUUID();
          redirectUriBuilder.queryParam("code", code).queryParam("state", state);
        }
        case "invalid_token" -> {
          var code = UUID.randomUUID();
          issuedCodes.put(code, Outcome.INVALID_TOKEN);
          redirectUriBuilder.queryParam("code", code).queryParam("state", state);
        }
        case "api_fail" -> {
          var code = UUID.randomUUID();
          issuedCodes.put(code, Outcome.FAIL);
          redirectUriBuilder.queryParam("code", code).queryParam("state", state);
        }
        case "slow_api" -> {
          var code = UUID.randomUUID();
          issuedCodes.put(code, Outcome.SLOW);
          redirectUriBuilder.queryParam("code", code).queryParam("state", state);
        }
        case "cancel" -> {
          log.info("Authorization cancelled by user");
          redirectUriBuilder
              .queryParam("error", "access_denied")
              .queryParam("error_description", "User cancelled authorization")
              .queryParam("state", state);
        }
        case "fail" -> {
          log.info("Authorization failed by user selection");
          redirectUriBuilder
              .queryParam("error", "server_error")
              .queryParam("error_description", "Authorization failed")
              .queryParam("state", state);
        }
        default -> throw new IllegalArgumentException("Unknown action: " + action);
      }
    }

    var redirectUriWithParams = redirectUriBuilder.build().toUri();
    return ResponseEntity.status(HttpStatus.FOUND).location(redirectUriWithParams).build();
  }

  @SuppressWarnings("checkstyle:RecordComponentName")
  record Token(String access_token, String token_type, long expires_in) {}

  @PostMapping("/v1/request-token")
  ResponseEntity<Token> requestToken(
      @RequestParam("code") UUID code,
      @RequestParam("redirect_uri") String redirectUri,
      @RequestParam("grant_type") String grantType,
      @RequestHeader("Authorization") String auth)
      throws JOSEException, InterruptedException {

    if (!auth.equals("Basic " + basicAuth)) {
      final var problem =
          ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "invalid_client");
      problem.setProperty("error", "invalid_client");
      return ResponseEntity.of(problem).build();
    }

    if (!"authorization_code".equals(grantType)) {
      final var problem =
          ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "unsupported_grant_type");
      problem.setProperty("error", "unsupported_grant_type");
      return ResponseEntity.of(problem).build();
    }

    Outcome outcome;
    if ((outcome = issuedCodes.remove(code)) == null) {
      final var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "invalid_grant");
      problem.setProperty("error", "invalid_grant");
      return ResponseEntity.of(problem).build();
    }

    Thread.sleep(500);

    final String id = UUID.randomUUID().toString();
    log.info("Creating authorization token with id: {}, code {}", id, code);

    String token;
    var exp = Date.from(Instant.now().plusSeconds(3600));

    if (outcome == Outcome.INVALID_TOKEN) {
      token = "invalid-token";
    } else {
      var claims =
          new JWTClaimsSet.Builder()
              .jwtID(id)
              .issuer("issuer")
              .issueTime(new Date())
              .expirationTime(exp)
              .build();

      // Create the signed JWT
      SignedJWT signedJwt =
          new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).build(), claims);
      signedJwt.sign(signer);

      token = signedJwt.serialize();
    }
    issuedTokens.put(token, outcome);
    return ResponseEntity.ok(new Token(token, "bearer", exp.getTime()));
  }

  @PutMapping("/v1/profile")
  ResponseEntity<Void> importProfile(
      @RequestHeader("Authorization") String auth,
      @RequestBody @Valid FullProfileDtoExternalPut profileDto)
      throws InterruptedException {

    return handleOperation(
        auth,
        () -> {
          profile = objectMapper.convertValue(profileDto, FullProfileDtoExternalGet.class);
          log.info(
              "Imported profile\n{}",
              objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profile));
          return ResponseEntity.noContent().build();
        });
  }

  @GetMapping("/v1/profile")
  ResponseEntity<FullProfileDtoExternalGet> exportProfile(
      @RequestHeader("Authorization") String auth) throws InterruptedException {

    return handleOperation(
        auth,
        () -> {
          try {
            if (profile == null) {
              try (var is = this.getClass().getResourceAsStream("/mock-profile.json")) {
                profile = objectMapper.readValue(is, FullProfileDtoExternalGet.class);
              }
            }
            log.info(
                "Exporting profile:\n{}",
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profile));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return ResponseEntity.ok(profile);
        });
  }

  <T> ResponseEntity<T> handleOperation(String auth, Supplier<ResponseEntity<T>> operation)
      throws InterruptedException {
    if (!auth.startsWith(BEARER_PREFIX)) {
      throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
    }
    String token = auth.substring(BEARER_PREFIX.length());
    Outcome outcome;

    if ((outcome = issuedTokens.remove(token)) == null) {
      return ResponseEntity.of(
              ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "invalid_token"))
          .build();
    }

    String jwtId;
    try {
      var jwt = SignedJWT.parse(token);
      jwt.verify(verifier);
      jwtId = jwt.getJWTClaimsSet().getJWTID();
    } catch (ParseException | JOSEException e) {
      log.info("Token verification failed for token {}", token);
      return ResponseEntity.of(
              ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "invalid_token"))
          .build();
    }

    if (outcome == Outcome.FAIL) {
      log.info("Simulating profile API failure for token {}", jwtId);
      return ResponseEntity.of(
              ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "API error"))
          .build();
    }

    if (outcome == Outcome.SLOW) {
      Thread.sleep(10000);
    } else {
      Thread.sleep(1000);
    }

    return operation.get();
  }
}
