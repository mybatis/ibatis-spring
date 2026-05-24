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
package org.springframework.orm.ibatis.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Additional tests for {@link AbstractLobTypeHandler} covering error paths not exercised by {@link LobTypeHandlerTests}
 * (which has an {@code @AfterEach} that verifies LobCreator.close()).
 */
@SuppressWarnings("deprecation")
class AbstractLobTypeHandlerAdditionalTests {

  // ---- Constructor guards ----

  @Test
  void testConstructorWithNullLobHandlerThrowsIllegalStateException() {
    assertThrows(IllegalStateException.class, () -> new ClobStringTypeHandler(null));
  }

  @Test
  void testDefaultConstructorWithNoConfigTimeLobHandlerThrows() {
    // getConfigTimeLobHandler() returns null outside of factory-bean initialisation, so the
    // default no-arg constructor must throw IllegalStateException.
    assertThrows(IllegalStateException.class, ClobStringTypeHandler::new);
  }

  // ---- setParameter without active transaction synchronization ----

  @Test
  void testSetParameterWithoutSynchronizationThrowsIllegalStateException() {
    LobHandler lobHandler = mock(LobHandler.class);
    ClobStringTypeHandler type = new ClobStringTypeHandler(lobHandler);
    PreparedStatement ps = mock(PreparedStatement.class);

    assertFalse(TransactionSynchronizationManager.isSynchronizationActive(),
        "Synchronization must not be active for this test");

    assertThrows(IllegalStateException.class, () -> type.setParameter(ps, 1, "content", null));
  }

  // ---- getResult(CallableStatement, int) ----

  @Test
  void testGetResultFromCallableStatementThrowsSqlException() {
    LobHandler lobHandler = mock(LobHandler.class);
    ClobStringTypeHandler type = new ClobStringTypeHandler(lobHandler);
    CallableStatement cs = mock(CallableStatement.class);

    assertThrows(SQLException.class, () -> type.getResult(cs, 1));
  }

  // ---- resolveJdbcType utility method ----

  @Test
  void testResolveJdbcTypeWithNullReturnsEmptyString() {
    LobHandler lobHandler = mock(LobHandler.class);
    ClobStringTypeHandler type = new ClobStringTypeHandler(lobHandler);
    assertEquals("", type.resolveJdbcType(null));
  }

  @Test
  void testResolveJdbcTypeWithValueReturnsValue() {
    LobHandler lobHandler = mock(LobHandler.class);
    ClobStringTypeHandler type = new ClobStringTypeHandler(lobHandler);
    assertEquals("CLOB", type.resolveJdbcType("CLOB"));
  }

}
