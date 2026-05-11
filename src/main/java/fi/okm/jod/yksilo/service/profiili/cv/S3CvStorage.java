/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili.cv;

import fi.okm.jod.yksilo.config.CvProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Profile("cloud")
@ConditionalOnProperty(
    name = "spring.cloud.aws.sqs.enabled",
    havingValue = "true",
    matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class S3CvStorage implements CvStorage {

  private final S3Client s3;
  private final CvProperties properties;

  @Override
  public String upload(UUID taskId, UUID userId, byte[] pdf) {
    var key = properties.s3Prefix() + "/" + userId + "/" + taskId + ".pdf";
    s3.putObject(
        PutObjectRequest.builder().bucket(properties.s3Bucket()).key(key).build(),
        RequestBody.fromContentProvider(
            ContentStreamProvider.fromByteArrayUnsafe(pdf), pdf.length, "application/pdf"));
    log.debug("Uploaded CV PDF to S3: {}", key);
    return key;
  }
}
