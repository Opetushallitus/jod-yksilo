/*
 * Copyright (c) 2025 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.admin;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import fi.okm.jod.yksilo.admin.dto.FullKoulutusData;
import fi.okm.jod.yksilo.admin.dto.KoulutusMahdollisuusResponseDto;
import fi.okm.jod.yksilo.admin.dto.RawKoulutusMahdollisuusDto;
import io.swagger.v3.oas.annotations.Operation;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
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

  private ObjectMapper objectMapper;

  private final S3Client s3Client;

  @Autowired
  public AdminController(final ObjectMapper objectMapper, final S3Client s3Client) {
    this.objectMapper = objectMapper;
    this.s3Client = s3Client;
  }

  @GetMapping("/koulutusmahdollisuudet")
  @Operation(
      summary = "Get all profiilit paged of by page and size",
      description = "Returns all profiilit basic information in JSON-format.")
  public List<KoulutusMahdollisuusResponseDto> getKoulutusMahdollisuudet() throws Exception {

    List<RawKoulutusMahdollisuusDto> koulutusMahdollisuudet =
        this.getJsonFromS3(
            "jod-devin-data", "koulutusmahdollisuudet/json_koulutusmahdollisuus.json");
    List<FullKoulutusData> koulutukset =
        this.getCsvFromS3(
            "jod-devin-data",
            "results/koulutus_cluster_with_individual_koulutukset_all_cols.csv",
            FullKoulutusData.class);
    return AdminMapper.toKoulutusMahdollisuusResponses(koulutusMahdollisuudet, koulutukset);
  }

  public List<RawKoulutusMahdollisuusDto> getJsonFromS3(final String bucketName, final String key)
      throws Exception {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(key).build();

    try (InputStream inputStream = s3Client.getObject(getObjectRequest)) {
      objectMapper.configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(), true);
      List<RawKoulutusMahdollisuusDto> mahdollisuudet =
          objectMapper.readValue(
              inputStream, new TypeReference<List<RawKoulutusMahdollisuusDto>>() {});
      return mahdollisuudet;
    }
  }

  public <T> List<T> getCsvFromS3(String bucketName, String key, Class<T> valueType)
      throws Exception {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(key).build();
    try (Reader inputStream = new InputStreamReader(s3Client.getObject(getObjectRequest))) {
      CsvToBean<T> csvToBean =
          new CsvToBeanBuilder<T>(inputStream)
              .withType(valueType)
              .withIgnoreLeadingWhiteSpace(true)
              .build();
      return csvToBean.parse();
    }
  }
}
