/*
 *    Copyright 2015-2026 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.springframework.orm.ibatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
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

  private static class TestRowHandler implements RowHandler {

    @Override
    public void handleRow(Object row) {
    }
  }

}
