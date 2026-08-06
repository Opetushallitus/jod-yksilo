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

  /**
   * Work experience at a single employer. In addition to the grouped format, the fields of the
   * legacy flat format, where each entry described a single position, are accepted, as messages in
   * the old format may still be queued.
   */
  record WorkExperience(
      String company,
      List<Position> positions,
      String title,
      @JsonProperty("start_date") LocalDate startDate,
      @JsonProperty("end_date") LocalDate endDate,
      String location,
      String description,
      List<String> skills) {

    WorkExperience(String company, List<Position> positions) {
      this(company, positions, null, null, null, null, null, null);
    }

    /** The positions, whether given in the grouped or the legacy flat format. */
    List<Position> allPositions() {
      if (positions != null) {
        return positions;
      }
      if (title == null && startDate == null) {
        return List.of();
      }
      return List.of(new Position(title, startDate, endDate, location, description, skills));
    }
  }

  record Position(
      String title,
      @JsonProperty("start_date") LocalDate startDate,
      @JsonProperty("end_date") LocalDate endDate,
      String location,
      String description,
      List<String> skills) {}

  /**
   * Education at a single institution. In addition to the grouped format, the fields of the legacy
   * flat format, where each entry described a single degree, are accepted, as messages in the old
   * format may still be queued.
   */
  record Education(
      String institution,
      List<Degree> degrees,
      String degree,
      @JsonProperty("start_date") LocalDate startDate,
      @JsonProperty("end_date") LocalDate endDate,
      String details) {

    Education(String institution, List<Degree> degrees) {
      this(institution, degrees, null, null, null, null);
    }

    /** The degrees, whether given in the grouped or the legacy flat format. */
    List<Degree> allDegrees() {
      if (degrees != null) {
        return degrees;
      }
      if (degree == null && startDate == null) {
        return List.of();
      }
      return List.of(new Degree(degree, startDate, endDate, details));
    }
  }

  record Degree(
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
