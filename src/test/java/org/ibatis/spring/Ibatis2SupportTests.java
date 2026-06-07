/*
 * SPDX-License-Identifier: Apache-2.0
 * See LICENSE file for details.
 *
 * Copyright 2015-2026 the original author or authors.
 */
package org.ibatis.spring;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Ibatis2Support}.
 */
class Ibatis2SupportTests {

  @Test
  void testInstantiation() {
    Ibatis2Support support = new Ibatis2Support();
    assertNotNull(support);
  }

}
