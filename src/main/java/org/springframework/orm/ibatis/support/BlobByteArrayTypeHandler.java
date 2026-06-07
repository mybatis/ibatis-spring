/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2015-2026 the original author or authors.
 */
package org.springframework.orm.ibatis.support;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * iBATIS TypeHandler implementation for byte arrays that get mapped to BLOBs. Retrieves the LobHandler to use from
 * SqlMapClientFactoryBean at config time.
 * <p>
 * Can also be defined in generic iBATIS mappings, as DefaultLobCreator will work with most JDBC-compliant database
 * drivers. In this case, the field type does not have to be BLOB: For databases like MySQL and MS SQL Server, any large
 * enough binary type will work.
 *
 * @author Juergen Hoeller
 *
 * @since 1.1.5
 *
 * @see org.springframework.orm.ibatis.SqlMapClientFactoryBean#setLobHandler
 *
 * @deprecated as of Spring 3.2, in favor of the native Spring support in the Mybatis follow-up project
 *             (https://mybatis.org/)
 */
@Deprecated
public class BlobByteArrayTypeHandler extends AbstractLobTypeHandler {

  /**
   * Constructor used by iBATIS: fetches config-time LobHandler from SqlMapClientFactoryBean.
   *
   * @see org.springframework.orm.ibatis.SqlMapClientFactoryBean#getConfigTimeLobHandler
   */
  public BlobByteArrayTypeHandler() {
    super();
  }

  /**
   * Constructor used for testing: takes an explicit LobHandler.
   *
   * @param lobHandler
   *          the LobHandler to use
   */
  protected BlobByteArrayTypeHandler(LobHandler lobHandler) {
    super(lobHandler);
  }

  @Override
  protected void setParameterInternal(PreparedStatement ps, int index, Object value, String jdbcType,
      LobCreator lobCreator) throws SQLException {
    lobCreator.setBlobAsBytes(ps, index, (byte[]) value);
  }

  @Override
  protected Object getResultInternal(ResultSet rs, int index, LobHandler lobHandler) throws SQLException {
    return lobHandler.getBlobAsBytes(rs, index);
  }

  @Override
  public Object valueOf(String s) {
    return s.getBytes();
  }

}
