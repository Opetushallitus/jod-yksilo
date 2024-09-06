/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config.datasource;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;

/** Custom Schema filter for Hibernate schema operations (hbm2ddl). */
@Slf4j
public class HibernateSchemaFilterProvider implements SchemaFilterProvider {

  private static final NamespaceExcludingSchemaFilter SCHEMA_FILTER =
      new NamespaceExcludingSchemaFilter("tyomahdollisuus_data");

  @Override
  public SchemaFilter getCreateFilter() {
    return SCHEMA_FILTER;
  }

  @Override
  public SchemaFilter getDropFilter() {
    return SCHEMA_FILTER;
  }

  @Override
  public SchemaFilter getTruncatorFilter() {
    return SCHEMA_FILTER;
  }

  @Override
  public SchemaFilter getMigrateFilter() {
    return SCHEMA_FILTER;
  }

  @Override
  public SchemaFilter getValidateFilter() {
    return SchemaFilter.ALL;
  }

  static class NamespaceExcludingSchemaFilter implements SchemaFilter {
    private final Set<String> excludedSchemas;

    NamespaceExcludingSchemaFilter(String... excludedSchemas) {
      this.excludedSchemas = Set.of(excludedSchemas);
    }

    @Override
    public boolean includeNamespace(Namespace namespace) {
      var verdict =
          namespace.getName().getSchema() == null
              || !excludedSchemas.contains(namespace.getName().getSchema().getCanonicalName());
      if (!verdict) {
        log.info("Excluding schema: {}", namespace.getName().getSchema().getCanonicalName());
      }
      return verdict;
    }

    @Override
    public boolean includeTable(Table table) {
      return true;
    }

    @Override
    public boolean includeSequence(Sequence sequence) {
      return true;
    }
  }
}
