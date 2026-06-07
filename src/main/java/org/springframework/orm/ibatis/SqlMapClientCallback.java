/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2015-2026 the original author or authors.
 */
package org.springframework.orm.ibatis;

import com.ibatis.sqlmap.client.SqlMapExecutor;

import java.sql.SQLException;

/**
 * Callback interface for data access code that works with the iBATIS {@link com.ibatis.sqlmap.client.SqlMapExecutor}
 * interface. To be used with {@link SqlMapClientTemplate}'s {@code execute} method, assumably often as anonymous
 * classes within a method implementation.
 *
 * @author Juergen Hoeller
 *
 * @since 24.02.2004
 *
 * @see SqlMapClientTemplate
 * @see org.springframework.jdbc.datasource.DataSourceTransactionManager
 *
 * @deprecated as of Spring 3.2, in favor of the native Spring support in the Mybatis follow-up project
 *             (https://mybatis.org/)
 */
@Deprecated
public interface SqlMapClientCallback<T> {

  /**
   * Gets called by {@code SqlMapClientTemplate.execute} with an active {@code SqlMapExecutor}. Does not need to care
   * about activating or closing the {@code SqlMapExecutor}, or handling transactions.
   * <p>
   * If called without a thread-bound JDBC transaction (initiated by DataSourceTransactionManager), the code will simply
   * get executed on the underlying JDBC connection with its transactional semantics. If using a JTA-aware DataSource,
   * the JDBC connection and thus the callback code will be transactional if a JTA transaction is active.
   * <p>
   * Allows for returning a result object created within the callback, i.e. a domain object or a collection of domain
   * objects. A thrown custom RuntimeException is treated as an application exception: It gets propagated to the caller
   * of the template.
   *
   * @param executor
   *          an active iBATIS SqlMapSession, passed-in as SqlMapExecutor interface here to avoid manual lifecycle
   *          handling
   *
   * @return a result object, or {@code null} if none
   *
   * @throws SQLException
   *           if thrown by the iBATIS SQL Maps API
   *
   * @see SqlMapClientTemplate#execute
   * @see SqlMapClientTemplate#executeWithListResult
   * @see SqlMapClientTemplate#executeWithMapResult
   */
  T doInSqlMapClient(SqlMapExecutor executor) throws SQLException;

}
