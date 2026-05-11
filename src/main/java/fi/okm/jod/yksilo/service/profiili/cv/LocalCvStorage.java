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
import fi.okm.jod.yksilo.service.ServiceException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "false")
@Slf4j
public class LocalCvStorage implements CvStorage {
  private final Path cvStoragePath;

  public LocalCvStorage(CvProperties cvProperties) throws IOException {
    this.cvStoragePath = Path.of(System.getProperty("java.io.tmpdir"), cvProperties.s3Prefix());
    Files.createDirectories(this.cvStoragePath);
    log.info("Local CV storage initialized: {}", cvStoragePath);
  }

  @Override
  public String upload(UUID taskId, UUID userId, byte[] pdf) {
    try {
      var file = cvStoragePath.resolve(taskId + ".pdf");
      Files.write(file, pdf);
      log.info("Saved CV PDF locally: {}", file.toAbsolutePath());
      return file.toAbsolutePath().toString();
    } catch (IOException e) {
      throw new ServiceException("Failed to save CV PDF locally", e);
    }
  }
}
