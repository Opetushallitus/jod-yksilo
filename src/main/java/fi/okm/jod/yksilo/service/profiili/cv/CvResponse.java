/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili.cv;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

/** Structured CV extraction response matching the CV schema. */
record CvResponse(
    @JsonProperty("work_experience") List<WorkExperience> workExperience,
    List<Education> education,
    @JsonProperty("other_activities") List<Activity> otherActivities) {

  record WorkExperience(
      String company,
      String title,
      @JsonProperty("start_date") LocalDate startDate,
      @JsonProperty("end_date") LocalDate endDate,
      String location,
      String description,
      List<String> skills) {}

  record Education(
      String institution,
      String degree,
      @JsonProperty("start_date") LocalDate startDate,
      @JsonProperty("end_date") LocalDate endDate,
      String details) {}

  record Activity(
      String category,
      String name,
      String description,
      @JsonProperty("start_date") LocalDate startDate,
      @JsonProperty("end_date") LocalDate endDate) {}
}
