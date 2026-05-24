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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.engine.transaction.TransactionConfig;
import com.ibatis.sqlmap.engine.transaction.external.ExternalTransactionConfig;

import java.util.Properties;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * Tests for {@link SqlMapClientFactoryBean} setters, validators, and accessors.
 */
@SuppressWarnings("deprecation")
class SqlMapClientFactoryBeanTests {

  // ---- getConfigTimeLobHandler ----

  @Test
  void testGetConfigTimeLobHandlerReturnsNullOutsideConfig() {
    assertNull(SqlMapClientFactoryBean.getConfigTimeLobHandler());
  }

  // ---- setTransactionConfigClass validation ----

  @Test
  void testSetTransactionConfigClassWithNullThrows() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    assertThrows(IllegalArgumentException.class, () -> factory.setTransactionConfigClass(null));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSetTransactionConfigClassWithNonTransactionConfigClassThrows() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    Class<? extends TransactionConfig> invalidClass = (Class<? extends TransactionConfig>) (Class<?>) String.class;
    assertThrows(IllegalArgumentException.class, () -> factory.setTransactionConfigClass(invalidClass));
  }

  @Test
  void testSetTransactionConfigClassWithValidClassSucceeds() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    factory.setTransactionConfigClass(ExternalTransactionConfig.class);
    // No exception expected
  }

  // ---- FactoryBean accessors before initialisation ----

  @Test
  void testGetObjectReturnsNullBeforeInit() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    assertNull(factory.getObject());
  }

  @Test
  void testGetObjectTypeReturnsSqlMapClientClassWhenNotInitialised() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    assertEquals(SqlMapClient.class, factory.getObjectType());
  }

  @Test
  void testIsSingletonReturnsTrue() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    assertTrue(factory.isSingleton());
  }

  // ---- Setter coverage ----

  @Test
  void testSetConfigLocation() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    Resource resource = mock(Resource.class);
    factory.setConfigLocation(resource);
    // No exception; verifies the single-Resource convenience setter code path
  }

  @Test
  void testSetConfigLocationWithNull() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    factory.setConfigLocation(null);
    // Null clears configLocations array; afterPropertiesSet would then throw
  }

  @Test
  void testSetConfigLocations() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    Resource[] locations = { mock(Resource.class), mock(Resource.class) };
    factory.setConfigLocations(locations);
    // No exception
  }

  @Test
  void testSetMappingLocations() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    Resource[] locations = { mock(Resource.class) };
    factory.setMappingLocations(locations);
    // No exception
  }

  @Test
  void testSetSqlMapClientProperties() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    Properties props = new Properties();
    props.setProperty("key", "value");
    factory.setSqlMapClientProperties(props);
    // No exception
  }

  @Test
  void testSetDataSource() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    DataSource ds = mock(DataSource.class);
    factory.setDataSource(ds);
    // No exception
  }

  @Test
  void testSetUseTransactionAwareDataSource() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    factory.setUseTransactionAwareDataSource(false);
    factory.setUseTransactionAwareDataSource(true);
    // No exception
  }

  @Test
  void testSetTransactionConfigProperties() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    Properties props = new Properties();
    props.setProperty("DefaultAutoCommit", "true");
    factory.setTransactionConfigProperties(props);
    // No exception
  }

  @Test
  void testSetLobHandler() {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    LobHandler lobHandler = mock(LobHandler.class);
    factory.setLobHandler(lobHandler);
    // No exception
  }

  // ---- afterPropertiesSet error path ----

  @Test
  void testAfterPropertiesSetWithNullConfigLocationsThrows() throws Exception {
    SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
    factory.setConfigLocation(null);
    assertThrows(IllegalArgumentException.class, factory::afterPropertiesSet);
  }

}
