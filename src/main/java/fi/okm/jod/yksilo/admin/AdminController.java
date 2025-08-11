/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/** TODO */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

  @Autowired private ObjectMapper objectMapper;

  @Autowired private final S3Client s3Client;

  @GetMapping("/koulutusmahdollisuudet")
  @Operation(
      summary = "Get all profiilit paged of by page and size",
      description = "Returns all profiilit basic information in JSON-format.")
  public Object getKoulutusMahdollisuudet() throws Exception {
    return this.getJsonFromS3(
        "jod-devin-data",
        "koulutusmahdollisuudet/full_json_lines_koulutusmahdollisuus.json",
        Object.class);
  }

  public <T> T getJsonFromS3(String bucketName, String key, Class<T> valueType) throws Exception {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(key).build();

    try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
      return objectMapper.readValue(inputStream, valueType);
    }
  }
}
