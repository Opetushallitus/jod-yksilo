/*
 * Copyright (c) 2026 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.service.profiili.cv;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/** Structured CV extraction response matching the CV schema. */
record CvResponse(
    @JsonProperty("work_experience") List<WorkExperience> workExperience,
    List<Education> education,
    @JsonProperty("other_activities") List<Activity> otherActivities) {

  record WorkExperience(String company, List<Position> positions) {}

  record Position(
      String title,
      @JsonProperty("start_date") LocalDate startDate,
      @JsonProperty("end_date") LocalDate endDate,
      String location,
      String description,
      @JsonProperty("skill_summary") List<String> skillSummary,
      @JsonProperty("esco_skills") List<EscoSkill> escoSkills) {}

  /**
   * Education at a single institution. The field names of the previous format ({@code degrees},
   * {@code degree}, {@code details}) are accepted as aliases, as messages in that format may still
   * be queued.
   */
  record Education(String institution, @JsonAlias("degrees") List<Entry> entries) {}

  record Entry(
      @JsonAlias("degree") String title,
      @JsonProperty("start_date") LocalDate startDate,
      @JsonProperty("end_date") LocalDate endDate,
      @JsonAlias("details") String description,
      @JsonProperty("skill_summary") List<String> skillSummary,
      @JsonProperty("esco_skills") List<EscoSkill> escoSkills) {}

  record EscoSkill(URI uri, String label, Double score) {}

  record Activity(
      String category,
      String name,
      String description,
      @JsonProperty("start_date") LocalDate startDate,
      @JsonProperty("end_date") LocalDate endDate) {}
}
