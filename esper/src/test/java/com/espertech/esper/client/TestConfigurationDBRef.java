/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.client;

import junit.framework.TestCase;

public class TestConfigurationDBRef extends TestCase {
    public void testTypeMapping() {
        tryInvalid("sometype", "Unsupported java type 'sometype' when expecting any of: [String, BigDecimal, Boolean, Byte, Short, Int, Long, Float, Double, ByteArray, SqlDate, SqlTime, SqlTimestamp]");

        ConfigurationDBRef config = new ConfigurationDBRef();
        config.addSqlTypesBinding(1, "int");
    }

    private void tryInvalid(String type, String text) {
        try {
            ConfigurationDBRef config = new ConfigurationDBRef();
            config.addSqlTypesBinding(1, type);
        } catch (ConfigurationException ex) {
            assertEquals(text, ex.getMessage());
        }
    }
}
