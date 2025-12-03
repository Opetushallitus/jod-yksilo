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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import fi.okm.jod.yksilo.external.tmt.model.FullProfileDtoExternalGet;
import fi.okm.jod.yksilo.external.tmt.model.FullProfileDtoExternalPut;
import jakarta.validation.Valid;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootApplication
@RestController
@Slf4j
public class Application {

  public static final String BEARER_PREFIX = "Bearer ";
  private final ObjectMapper objectMapper;
  private final Set<String> issuedTokens = ConcurrentHashMap.newKeySet();
  private final Set<UUID> issuedCodes = ConcurrentHashMap.newKeySet();
  private final JWSSigner signer;

  @SuppressWarnings("java:S3077")
  private volatile FullProfileDtoExternalGet profile;

  public static void main(String[] args) {
    log.info("Starting TMT Mock API");
    SpringApplication.run(Application.class, args);
  }

  Application(Jackson2ObjectMapperBuilder objectMapperBuilder)
      throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, JOSEException {
    var keyGenerator = KeyPairGenerator.getInstance("EC");
    var curve = new ECGenParameterSpec("secp256r1");
    keyGenerator.initialize(curve);
    var keyPair = keyGenerator.generateKeyPair();
    this.objectMapper = objectMapperBuilder.serializationInclusion(Include.NON_EMPTY).build();
    this.signer = new ECDSASigner((ECPrivateKey) keyPair.getPrivate());
  }

  @GetMapping("/authorize")
  ResponseEntity<Void> authorize(
      @RequestParam("redirect_uri") String redirectUri, @RequestParam("state") String state) {
    log.info("Received authorization request, redirect {}", redirectUri);
    var code = UUID.randomUUID();
    log.info("Issuing authorization code: {}", code);
    issuedCodes.add(code);

    var redirectUriWithParams =
        UriComponentsBuilder.fromUriString(redirectUri)
            .queryParam("code", code)
            .queryParam("state", state)
            .build()
            .toUri();
    return ResponseEntity.status(HttpStatus.FOUND).location(redirectUriWithParams).build();
  }

  @SuppressWarnings("checkstyle:RecordComponentName")
  record Token(String access_token, String token_type, long expires_in) {}

  @PostMapping("/v1/request-token")
  ResponseEntity<Token> requestToken(@RequestParam("code") UUID code) throws JOSEException {

    if (!issuedCodes.remove(code)) {
      throw new IllegalArgumentException("Unknown authorization code" + code);
    }

    final String id = UUID.randomUUID().toString();
    log.info("Creating authorization token with id: {}, code {}", id, code);

    var exp = Date.from(Instant.now().plusSeconds(3600));
    var claims =
        new JWTClaimsSet.Builder()
            .jwtID(id)
            .issuer("issuer")
            .issueTime(new Date())
            .expirationTime(exp)
            .build();

    // Create the signed JWT
    SignedJWT signedJwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.ES256).build(), claims);
    signedJwt.sign(signer);

    var token = signedJwt.serialize();
    issuedTokens.add(token);

    return ResponseEntity.ok(new Token(token, "bearer", exp.getTime()));
  }

  @PutMapping("/v1/profile")
  void importProfile(
      @RequestHeader("Authorization") String auth,
      @RequestBody @Valid FullProfileDtoExternalPut profileDto)
      throws JsonProcessingException {
    if (!auth.startsWith(BEARER_PREFIX)) {
      throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
    }
    String token = auth.substring(BEARER_PREFIX.length());
    if (!issuedTokens.remove(token)) {
      throw new IllegalArgumentException("Invalid or expired token");
    }
    log.info(
        "Updating profile: {}",
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profileDto));

    profile = objectMapper.convertValue(profileDto, FullProfileDtoExternalGet.class);
  }

  @GetMapping("/v1/profile")
  FullProfileDtoExternalGet exportProfile(@RequestHeader("Authorization") String auth) {
    if (!auth.startsWith(BEARER_PREFIX)) {
      throw new IllegalArgumentException("Authorization header must start with 'Bearer '");
    }
    String token = auth.substring(BEARER_PREFIX.length());
    if (!issuedTokens.remove(token)) {
      throw new IllegalArgumentException("Invalid or expired token");
    }
    return profile;
  }
}
