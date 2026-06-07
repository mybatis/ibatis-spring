/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2015-2026 the original author or authors.
 */
package org.springframework.orm.ibatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapExecutor;
import com.ibatis.sqlmap.client.SqlMapSession;
import com.ibatis.sqlmap.client.event.RowHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;

/**
 * Tests for {@link SqlMapClientTemplate} and related ibatis integration classes.
 *
 * @author Juergen Hoeller
 * @author Alef Arendsen
 * @author Phillip Webb
 */
@SuppressWarnings({ "deprecation", "rawtypes" })
class SqlMapClientTests {

  @Test
  void testSqlMapClientFactoryBeanWithoutConfig() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    factory.setConfigLocation(null);
    assertThrows(IllegalArgumentException.class, factory::afterPropertiesSet);
  }

  @Test
  void testSqlMapClientTemplate() throws SQLException {
    DataSource ds = mock(DataSource.class);
    Connection con = mock(Connection.class);
    final SqlMapSession session = mock(SqlMapSession.class);
    SqlMapClient client = mock(SqlMapClient.class);

    given(ds.getConnection()).willReturn(con);
    given(client.openSession()).willReturn(session);

    SqlMapClientTemplate template = new SqlMapClientTemplate();
    template.setDataSource(ds);
    template.setSqlMapClient(client);
    template.afterPropertiesSet();
    Object result = template.execute(executor -> {
      assertTrue(executor == session);
      return "done";
    });
    assertEquals("done", result);

    verify(con).close();
    verify(session).setUserConnection(con);
    verify(session).close();
  }

  @Test
  void testSqlMapClientTemplateWithNestedSqlMapSession() throws SQLException {
    DataSource ds = mock(DataSource.class);
    final Connection con = mock(Connection.class);
    final SqlMapSession session = mock(SqlMapSession.class);
    SqlMapClient client = mock(SqlMapClient.class);

    given(client.openSession()).willReturn(session);
    given(session.getCurrentConnection()).willReturn(con);

    SqlMapClientTemplate template = new SqlMapClientTemplate();
    template.setDataSource(ds);
    template.setSqlMapClient(client);
    template.afterPropertiesSet();
    Object result = template.execute(executor -> {
      assertTrue(executor == session);
      return "done";
    });
    assertEquals("done", result);
  }

  @Test
  void testQueryForObjectOnSqlMapSession() throws SQLException {
    DataSource ds = mock(DataSource.class);
    Connection con = mock(Connection.class);
    SqlMapClient client = mock(SqlMapClient.class);
    SqlMapSession session = mock(SqlMapSession.class);

    given(ds.getConnection()).willReturn(con);
    given(client.getDataSource()).willReturn(ds);
    given(client.openSession()).willReturn(session);
    given(session.queryForObject("myStatement", "myParameter")).willReturn("myResult");

    SqlMapClientTemplate template = new SqlMapClientTemplate();
    template.setSqlMapClient(client);
    template.afterPropertiesSet();
    assertEquals("myResult", template.queryForObject("myStatement", "myParameter"));

    verify(con).close();
    verify(session).setUserConnection(con);
    verify(session).close();
  }

  @Test
  void testQueryForObject() throws SQLException {
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.queryForObject("myStatement", null)).willReturn("myResult");
    assertEquals("myResult", template.queryForObject("myStatement"));
  }

  @Test
  void testQueryForObjectWithParameter() throws SQLException {
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.queryForObject("myStatement", "myParameter")).willReturn("myResult");
    assertEquals("myResult", template.queryForObject("myStatement", "myParameter"));
  }

  @Test
  void testQueryForObjectWithParameterAndResultObject() throws SQLException {
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.queryForObject("myStatement", "myParameter", "myResult")).willReturn("myResult");
    assertEquals("myResult", template.queryForObject("myStatement", "myParameter", "myResult"));
  }

  @Test
  void testQueryForList() throws SQLException {
    List result = new ArrayList();
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.queryForList("myStatement", null)).willReturn(result);
    assertEquals(result, template.queryForList("myStatement"));
  }

  @Test
  void testQueryForListWithParameter() throws SQLException {
    List result = new ArrayList();
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.queryForList("myStatement", "myParameter")).willReturn(result);
    assertEquals(result, template.queryForList("myStatement", "myParameter"));
  }

  @Test
  void testQueryForListWithResultSize() throws SQLException {
    List result = new ArrayList();
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.queryForList("myStatement", null, 10, 20)).willReturn(result);
    assertEquals(result, template.queryForList("myStatement", 10, 20));
  }

  @Test
  void testQueryForListParameterAndWithResultSize() throws SQLException {
    List result = new ArrayList();
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.queryForList("myStatement", "myParameter", 10, 20)).willReturn(result);
    assertEquals(result, template.queryForList("myStatement", "myParameter", 10, 20));
  }

  @Test
  void testQueryWithRowHandler() throws SQLException {
    RowHandler rowHandler = new TestRowHandler();
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    template.queryWithRowHandler("myStatement", rowHandler);
    verify(template.executor).queryWithRowHandler("myStatement", null, rowHandler);
  }

  @Test
  void testQueryWithRowHandlerWithParameter() throws SQLException {
    RowHandler rowHandler = new TestRowHandler();
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    template.queryWithRowHandler("myStatement", "myParameter", rowHandler);
    verify(template.executor).queryWithRowHandler("myStatement", "myParameter", rowHandler);
  }

  @Test
  void testQueryForMap() throws SQLException {
    Map result = new HashMap();
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.queryForMap("myStatement", "myParameter", "myKey")).willReturn(result);
    assertEquals(result, template.queryForMap("myStatement", "myParameter", "myKey"));
  }

  @Test
  void testQueryForMapWithValueProperty() throws SQLException {
    Map result = new HashMap();
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.queryForMap("myStatement", "myParameter", "myKey", "myValue")).willReturn(result);
    assertEquals(result, template.queryForMap("myStatement", "myParameter", "myKey", "myValue"));
  }

  @Test
  void testInsert() throws SQLException {
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.insert("myStatement", null)).willReturn("myResult");
    assertEquals("myResult", template.insert("myStatement"));
  }

  @Test
  void testInsertWithParameter() throws SQLException {
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.insert("myStatement", "myParameter")).willReturn("myResult");
    assertEquals("myResult", template.insert("myStatement", "myParameter"));
  }

  @Test
  void testUpdate() throws SQLException {
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.update("myStatement", null)).willReturn(10);
    assertEquals(10, template.update("myStatement"));
  }

  @Test
  void testUpdateWithParameter() throws SQLException {
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.update("myStatement", "myParameter")).willReturn(10);
    assertEquals(10, template.update("myStatement", "myParameter"));
  }

  @Test
  void testUpdateWithRequiredRowsAffected() throws SQLException {
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.update("myStatement", "myParameter")).willReturn(10);
    template.update("myStatement", "myParameter", 10);
    verify(template.executor).update("myStatement", "myParameter");
  }

  @Test
  void testUpdateWithRequiredRowsAffectedAndInvalidRowCount() throws SQLException {
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.update("myStatement", "myParameter")).willReturn(20);
    JdbcUpdateAffectedIncorrectNumberOfRowsException ex = assertThrows(
        JdbcUpdateAffectedIncorrectNumberOfRowsException.class,
        () -> template.update("myStatement", "myParameter", 10));
    assertEquals(10, ex.getExpectedRowsAffected());
    assertEquals(20, ex.getActualRowsAffected());
  }

  @Test
  void testDelete() throws SQLException {
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.delete("myStatement", null)).willReturn(10);
    assertEquals(10, template.delete("myStatement"));
  }

  @Test
  void testDeleteWithParameter() throws SQLException {
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.delete("myStatement", "myParameter")).willReturn(10);
    assertEquals(10, template.delete("myStatement", "myParameter"));
  }

  @Test
  void testDeleteWithRequiredRowsAffected() throws SQLException {
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.delete("myStatement", "myParameter")).willReturn(10);
    template.delete("myStatement", "myParameter", 10);
    verify(template.executor).delete("myStatement", "myParameter");
  }

  @Test
  void testDeleteWithRequiredRowsAffectedAndInvalidRowCount() throws SQLException {
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    given(template.executor.delete("myStatement", "myParameter")).willReturn(20);
    JdbcUpdateAffectedIncorrectNumberOfRowsException ex = assertThrows(
        JdbcUpdateAffectedIncorrectNumberOfRowsException.class,
        () -> template.delete("myStatement", "myParameter", 10));
    assertEquals(10, ex.getExpectedRowsAffected());
    assertEquals(20, ex.getActualRowsAffected());
  }

  @Test
  void testSqlMapClientDaoSupport() throws Exception {
    DataSource ds = mock(DataSource.class);
    SqlMapClientDaoSupport testDao = new SqlMapClientDaoSupport() {
    };
    testDao.setDataSource(ds);
    assertEquals(ds, testDao.getDataSource());

    SqlMapClient client = mock(SqlMapClient.class);

    testDao.setSqlMapClient(client);
    assertEquals(client, testDao.getSqlMapClient());

    SqlMapClientTemplate template = new SqlMapClientTemplate();
    template.setDataSource(ds);
    template.setSqlMapClient(client);
    testDao.setSqlMapClientTemplate(template);
    assertEquals(template, testDao.getSqlMapClientTemplate());

    testDao.afterPropertiesSet();
  }

  // ---- SqlMapClientTemplate additional constructor / configuration tests ----

  @Test
  void testSqlMapClientTemplateConstructorWithSqlMapClient() {
    SqlMapClient client = mock(SqlMapClient.class);
    DataSource ds = mock(DataSource.class);
    given(client.getDataSource()).willReturn(ds);
    SqlMapClientTemplate template = new SqlMapClientTemplate(client);
    assertEquals(client, template.getSqlMapClient());
  }

  @Test
  void testSqlMapClientTemplateConstructorWithDataSourceAndSqlMapClient() {
    DataSource ds = mock(DataSource.class);
    SqlMapClient client = mock(SqlMapClient.class);
    given(client.getDataSource()).willReturn(ds);
    SqlMapClientTemplate template = new SqlMapClientTemplate(ds, client);
    assertEquals(client, template.getSqlMapClient());
    assertEquals(ds, template.getDataSource());
  }

  @Test
  void testSqlMapClientTemplateAfterPropertiesSetWithoutSqlMapClient() {
    SqlMapClientTemplate template = new SqlMapClientTemplate();
    assertThrows(IllegalArgumentException.class, template::afterPropertiesSet);
  }

  @Test
  void testSqlMapClientTemplateGetDataSourceFallsBackToClientDataSource() {
    SqlMapClient client = mock(SqlMapClient.class);
    DataSource clientDs = mock(DataSource.class);
    given(client.getDataSource()).willReturn(clientDs);
    SqlMapClientTemplate template = new SqlMapClientTemplate();
    template.setSqlMapClient(client);
    assertEquals(clientDs, template.getDataSource());
  }

  // ---- SqlMapClientTemplate execute() error-path tests ----

  @Test
  void testExecuteThrowsCannotGetJdbcConnectionExceptionOnConnectionFailure() throws SQLException {
    DataSource ds = mock(DataSource.class);
    SqlMapSession session = mock(SqlMapSession.class);
    SqlMapClient client = mock(SqlMapClient.class);

    given(client.openSession()).willReturn(session);
    given(ds.getConnection()).willThrow(new SQLException("connection refused"));

    SqlMapClientTemplate template = new SqlMapClientTemplate();
    template.setDataSource(ds);
    template.setSqlMapClient(client);
    template.afterPropertiesSet();

    assertThrows(CannotGetJdbcConnectionException.class, () -> template.execute(executor -> null));
    verify(session).close();
  }

  @Test
  void testExecuteWithTransactionAwareDataSource() throws SQLException {
    DataSource targetDs = mock(DataSource.class);
    Connection con = mock(Connection.class);
    SqlMapSession session = mock(SqlMapSession.class);
    SqlMapClient client = mock(SqlMapClient.class);

    given(targetDs.getConnection()).willReturn(con);
    given(client.openSession()).willReturn(session);

    TransactionAwareDataSourceProxy ds = new TransactionAwareDataSourceProxy(targetDs);

    SqlMapClientTemplate template = new SqlMapClientTemplate();
    template.setDataSource(ds);
    template.setSqlMapClient(client);
    template.afterPropertiesSet();

    Object result = template.execute(executor -> "done");
    assertEquals("done", result);
    verify(session).close();
  }

  @Test
  void testExecuteTranslatesCallbackSqlException() throws SQLException {
    DataSource ds = mock(DataSource.class);
    Connection con = mock(Connection.class);
    SqlMapSession session = mock(SqlMapSession.class);
    SqlMapClient client = mock(SqlMapClient.class);

    given(ds.getConnection()).willReturn(con);
    given(client.openSession()).willReturn(session);

    SqlMapClientTemplate template = new SqlMapClientTemplate();
    template.setDataSource(ds);
    template.setSqlMapClient(client);
    template.afterPropertiesSet();

    assertThrows(DataAccessException.class, () -> template.execute(executor -> {
      throw new SQLException("query failed", "42000");
    }));
    verify(session).close();
  }

  // ---- Deprecated execute-with-typed-result delegates ----

  @Test
  @SuppressWarnings("deprecation")
  void testExecuteWithListResult() {
    List<String> expected = new ArrayList<>();
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    List actual = template.executeWithListResult(executor -> expected);
    assertEquals(expected, actual);
  }

  @Test
  @SuppressWarnings("deprecation")
  void testExecuteWithMapResult() {
    Map<String, String> expected = new HashMap<>();
    TestSqlMapClientTemplate template = new TestSqlMapClientTemplate();
    Map actual = template.executeWithMapResult(executor -> expected);
    assertEquals(expected, actual);
  }

  // ---- Null-result guards in update/delete ----

  @Test
  void testUpdateReturnsZeroWhenExecuteReturnsNull() {
    NullExecuteTemplate template = new NullExecuteTemplate();
    assertEquals(0, template.update("myStatement"));
    assertEquals(0, template.update("myStatement", "myParam"));
  }

  @Test
  void testDeleteReturnsZeroWhenExecuteReturnsNull() {
    NullExecuteTemplate template = new NullExecuteTemplate();
    assertEquals(0, template.delete("myStatement"));
    assertEquals(0, template.delete("myStatement", "myParam"));
  }

  // ---- SqlMapClientDaoSupport additional branch coverage ----

  @Test
  void testSqlMapClientDaoSupportExternalTemplateIgnoresSetDataSourceAndSetSqlMapClient() throws Exception {
    SqlMapClientTemplate externalTemplate = mock(SqlMapClientTemplate.class);

    SqlMapClientDaoSupport testDao = new SqlMapClientDaoSupport() {
    };
    testDao.setSqlMapClientTemplate(externalTemplate);

    DataSource ds = mock(DataSource.class);
    SqlMapClient client = mock(SqlMapClient.class);

    testDao.setDataSource(ds);
    testDao.setSqlMapClient(client);

    verify(externalTemplate, never()).setDataSource(ds);
    verify(externalTemplate, never()).setSqlMapClient(client);

    assertEquals(externalTemplate, testDao.getSqlMapClientTemplate());
  }

  @Test
  void testSqlMapClientDaoSupportCheckDaoConfigCallsInternalTemplateAfterPropertiesSet() throws Exception {
    DataSource ds = mock(DataSource.class);
    SqlMapClient client = mock(SqlMapClient.class);
    given(client.getDataSource()).willReturn(ds);

    SqlMapClientDaoSupport testDao = new SqlMapClientDaoSupport() {
    };
    testDao.setSqlMapClient(client);
    assertNotNull(testDao.getSqlMapClientTemplate());
    testDao.afterPropertiesSet();
    assertEquals(client, testDao.getSqlMapClient());
  }

  @Test
  void testExecuteConnectionCloseExceptionIsSwallowed() throws SQLException {
    DataSource ds = mock(DataSource.class);
    Connection con = mock(Connection.class);
    SqlMapSession session = mock(SqlMapSession.class);
    SqlMapClient client = mock(SqlMapClient.class);

    given(ds.getConnection()).willReturn(con);
    given(client.openSession()).willReturn(session);
    // Make connection.close() throw so the catch(Throwable) path in execute() is exercised
    willThrow(new RuntimeException("close failed")).given(con).close();

    SqlMapClientTemplate template = new SqlMapClientTemplate();
    template.setDataSource(ds);
    template.setSqlMapClient(client);
    template.afterPropertiesSet();

    // The exception from close() must be swallowed (logged at debug level), not rethrown
    Object result = template.execute(executor -> "done");
    assertEquals("done", result);
    verify(session).close();
  }

  private static class TestSqlMapClientTemplate extends SqlMapClientTemplate {

    public SqlMapExecutor executor = mock(SqlMapExecutor.class);

    @Override
    public <T> T execute(SqlMapClientCallback<T> action) throws DataAccessException {
      try {
        return action.doInSqlMapClient(executor);
      } catch (SQLException ex) {
        throw getExceptionTranslator().translate("SqlMapClient operation", null, ex);
      }
    }
  }

  /**
   * Template override whose {@code execute} always returns {@code null}; used to exercise the
   * {@code result != null ? result : 0} guards in {@code update} and {@code delete}.
   */
  private static class NullExecuteTemplate extends SqlMapClientTemplate {

    @Override
    public <T> T execute(SqlMapClientCallback<T> action) throws DataAccessException {
      return null;
    }
  }

  private static class TestRowHandler implements RowHandler {

    @Override
    public void handleRow(Object row) {
    }
  }

}
