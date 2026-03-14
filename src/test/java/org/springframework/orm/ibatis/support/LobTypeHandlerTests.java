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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Tests for iBATIS LOB type handler implementations.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
@SuppressWarnings("deprecation")
class LobTypeHandlerTests {

  private final ResultSet rs = mock(ResultSet.class);
  private final PreparedStatement ps = mock(PreparedStatement.class);

  private final LobHandler lobHandler = mock(LobHandler.class);
  private final LobCreator lobCreator = mock(LobCreator.class);

  @BeforeEach
  void setUp() throws Exception {
    given(rs.findColumn("column")).willReturn(1);
    given(lobHandler.getLobCreator()).willReturn(lobCreator);
  }

  @AfterEach
  void tearDown() {
    verify(lobCreator).close();
    assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
    assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
  }

  @Test
  void testClobStringTypeHandler() throws Exception {
    given(lobHandler.getClobAsString(rs, 1)).willReturn("content");

    ClobStringTypeHandler type = new ClobStringTypeHandler(lobHandler);
    assertEquals("content", type.valueOf("content"));
    assertEquals("content", type.getResult(rs, "column"));
    assertEquals("content", type.getResult(rs, 1));

    TransactionSynchronizationManager.initSynchronization();
    try {
      type.setParameter(ps, 1, "content", null);
      List<TransactionSynchronization> synchs = TransactionSynchronizationManager.getSynchronizations();
      assertEquals(1, synchs.size());
      assertTrue(synchs.get(0).getClass().getName().endsWith("LobCreatorSynchronization"));
      synchs.get(0).beforeCompletion();
      synchs.get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
    verify(lobCreator).setClobAsString(ps, 1, "content");
  }

  @Test
  void testClobStringTypeWithSynchronizedConnection() throws Exception {
    DataSource dsTarget = mock(DataSource.class);
    Connection defaultCon = mock(Connection.class);
    Connection txCon = mock(Connection.class);
    given(dsTarget.getConnection()).willReturn(defaultCon, txCon);
    DataSource ds = new LazyConnectionDataSourceProxy(dsTarget);

    given(lobHandler.getClobAsString(rs, 1)).willReturn("content");

    ClobStringTypeHandler type = new ClobStringTypeHandler(lobHandler);
    assertEquals("content", type.valueOf("content"));
    assertEquals("content", type.getResult(rs, "column"));
    assertEquals("content", type.getResult(rs, 1));

    TransactionSynchronizationManager.initSynchronization();
    try {
      DataSourceUtils.getConnection(ds);
      type.setParameter(ps, 1, "content", null);
      List<TransactionSynchronization> synchs = TransactionSynchronizationManager.getSynchronizations();
      assertEquals(2, synchs.size());
      assertTrue(synchs.get(0).getClass().getName().endsWith("LobCreatorSynchronization"));
      synchs.get(0).beforeCompletion();
      synchs.get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
      synchs.get(1).beforeCompletion();
      synchs.get(1).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
    verify(lobCreator).setClobAsString(ps, 1, "content");
  }

  @Test
  void testBlobByteArrayType() throws Exception {
    byte[] content = "content".getBytes();
    given(lobHandler.getBlobAsBytes(rs, 1)).willReturn(content);

    BlobByteArrayTypeHandler type = new BlobByteArrayTypeHandler(lobHandler);
    assertArrayEquals(content, (byte[]) type.valueOf("content"));
    assertEquals(content, type.getResult(rs, "column"));
    assertEquals(content, type.getResult(rs, 1));

    TransactionSynchronizationManager.initSynchronization();
    try {
      type.setParameter(ps, 1, content, null);
      List<TransactionSynchronization> synchs = TransactionSynchronizationManager.getSynchronizations();
      assertEquals(1, synchs.size());
      synchs.get(0).beforeCompletion();
      synchs.get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
    verify(lobCreator).setBlobAsBytes(ps, 1, content);
  }

  @Test
  void testBlobSerializableType() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject("content");
    }

    given(lobHandler.getBlobAsBinaryStream(rs, 1))
        .willAnswer(invocation -> new ByteArrayInputStream(baos.toByteArray()));

    BlobSerializableTypeHandler type = new BlobSerializableTypeHandler(lobHandler);
    assertEquals("content", type.valueOf("content"));
    assertEquals("content", type.getResult(rs, "column"));
    assertEquals("content", type.getResult(rs, 1));

    TransactionSynchronizationManager.initSynchronization();
    try {
      type.setParameter(ps, 1, "content", null);
      List<TransactionSynchronization> synchs = TransactionSynchronizationManager.getSynchronizations();
      assertEquals(1, synchs.size());
      synchs.get(0).beforeCompletion();
      synchs.get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
    verify(lobCreator).setBlobAsBytes(ps, 1, baos.toByteArray());
  }

  @Test
  void testBlobSerializableTypeWithNull() throws Exception {
    given(lobHandler.getBlobAsBinaryStream(rs, 1)).willReturn(null);

    BlobSerializableTypeHandler type = new BlobSerializableTypeHandler(lobHandler);
    assertNull(type.valueOf(null));
    assertNull(type.getResult(rs, "column"));
    assertNull(type.getResult(rs, 1));

    TransactionSynchronizationManager.initSynchronization();
    try {
      type.setParameter(ps, 1, null, null);
      List<TransactionSynchronization> synchs = TransactionSynchronizationManager.getSynchronizations();
      assertEquals(1, synchs.size());
      synchs.get(0).beforeCompletion();
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
    verify(lobCreator).setBlobAsBytes(ps, 1, null);
  }

}
