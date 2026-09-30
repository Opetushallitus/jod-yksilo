/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.aws;

import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

/**
 * Adds support for AWS Secrets Manager version stages to {@code spring.config.import} locations.
 *
 * <p>Spring Cloud AWS always requests the default ({@code AWSCURRENT}) version of a secret, and its
 * {@code aws-secretsmanager:} config data support offers no way to ask for another staging label.
 * This interceptor recognizes a {@code <secretId>@<versionStage>} suffix in the requested secret id
 * and rewrites the request accordingly, so that a location such as
 *
 * <pre>aws-secretsmanager:arn:aws:secretsmanager:...:secret:example@AWSPENDING?prefix=some.prefix.
 * </pre>
 *
 * <p>resolves the {@code AWSPENDING} version of the secret. Requests without the suffix are passed
 * through untouched.
 *
 * <p>The separator is the <em>last</em> {@code @} in the secret id, because {@code @} is a legal
 * character in secret names.
 */
public class SecretsManagerVersionStageInterceptor implements ExecutionInterceptor {

  static final char SEPARATOR = '@';

  @Override
  public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes attributes) {
    if (context.request() instanceof GetSecretValueRequest request
        && request.versionStage() == null
        && request.versionId() == null) {
      var secretId = request.secretId();
      var index = secretId == null ? -1 : secretId.lastIndexOf(SEPARATOR);
      if (index > 0 && index < secretId.length() - 1) {
        return request.toBuilder()
            .secretId(secretId.substring(0, index))
            .versionStage(secretId.substring(index + 1))
            .build();
      }
    }
    return context.request();
  }
}
