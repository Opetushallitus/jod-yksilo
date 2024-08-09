/*
 * Copyright (c) 2024 The Finnish Ministry of Education and Culture, The Finnish
 * The Ministry of Economic Affairs and Employment, The Finnish National Agency of
 * Education (Opetushallitus) and The Finnish Development and Administration centre
 * for ELY Centres and TE Offices (KEHA).
 *
 * Licensed under the EUPL-1.2-or-later.
 */

package fi.okm.jod.yksilo.config;

import fi.okm.jod.yksilo.domain.JodUser;
import jakarta.persistence.EntityManagerFactory;
import java.sql.SQLException;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;

/**
 * A JpaTransactionManager that sets the jod.yksilo_id session variable to the current user's id, to
 * enable RLS in PostgreSQL.
 */
@Component("transactionManager")
@SuppressWarnings("serial")
@Slf4j
class UserAwareJpaTransactionManager extends JpaTransactionManager {

  public UserAwareJpaTransactionManager() {
    super();
  }

  public UserAwareJpaTransactionManager(EntityManagerFactory emf) {
    super(emf);
  }

  @Override
  protected void doBegin(@Nonnull Object transaction, @Nonnull TransactionDefinition definition) {
    super.doBegin(transaction, definition);
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof JodUser user) {
      try (var stmt =
          ((JdbcTransactionObjectSupport) transaction)
              .getConnectionHolder()
              .getConnection()
              .prepareStatement("SELECT set_config('jod.yksilo_id', ?, true)")) {
        stmt.setString(1, user.getId().toString());
        stmt.execute();
      } catch (SQLException e) {
        throw new CannotCreateTransactionException("Failed to set config parameter", e);
      }
    }
  }
}
