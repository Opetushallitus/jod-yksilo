/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ListSecretsRequest;

class SecretsManagerVersionStageInterceptorTest {

  private static final String ARN =
      "arn:aws:secretsmanager:eu-west-1:123456789012:secret:jod/saml2-AbCdEf";

  private final SecretsManagerVersionStageInterceptor interceptor =
      new SecretsManagerVersionStageInterceptor();

  @Test
  void shouldExtractVersionStage() {
    var result =
        (GetSecretValueRequest)
            modify(GetSecretValueRequest.builder().secretId(ARN + "@AWSPENDING").build());

    assertEquals(ARN, result.secretId());
    assertEquals("AWSPENDING", result.versionStage());
  }

  @Test
  void shouldSplitOnLastSeparatorBecauseSecretNamesMayContainIt() {
    var secretId = ARN + "@v2";
    var result =
        (GetSecretValueRequest)
            modify(GetSecretValueRequest.builder().secretId(secretId + "@AWSPENDING").build());

    assertEquals(secretId, result.secretId());
    assertEquals("AWSPENDING", result.versionStage());
  }

  @ParameterizedTest
  @ValueSource(strings = {ARN, ARN + "@", "@AWSPENDING"})
  void shouldPassThroughWithoutUsableSuffix(String secretId) {
    var result =
        (GetSecretValueRequest) modify(GetSecretValueRequest.builder().secretId(secretId).build());

    assertEquals(secretId, result.secretId());
    assertNull(result.versionStage());
  }

  @Test
  void shouldNotOverrideExplicitVersionStage() {
    var request =
        GetSecretValueRequest.builder()
            .secretId(ARN + "@AWSPENDING")
            .versionStage("AWSCURRENT")
            .build();

    assertSame(request, modify(request));
  }

  @Test
  void shouldIgnoreOtherRequests() {
    var request = ListSecretsRequest.builder().build();

    assertSame(request, modify(request));
  }

  private SdkRequest modify(SdkRequest request) {
    return interceptor.modifyRequest(() -> request, new ExecutionAttributes());
  }
}
