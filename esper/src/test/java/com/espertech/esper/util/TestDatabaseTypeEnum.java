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
package com.espertech.esper.util;

import junit.framework.TestCase;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

public class TestDatabaseTypeEnum extends TestCase {
    public void testTypes() {
        Object[][] types = new Object[][]{
                {DatabaseTypeEnum.String, String.class},
                {DatabaseTypeEnum.BigDecimal, BigDecimal.class},
                {DatabaseTypeEnum.Boolean, Boolean.class},
                {DatabaseTypeEnum.Byte, Byte.class},
                {DatabaseTypeEnum.Short, Short.class},
                {DatabaseTypeEnum.Int, Integer.class},
                {DatabaseTypeEnum.Long, Long.class},
                {DatabaseTypeEnum.Float, Float.class},
                {DatabaseTypeEnum.Double, Double.class},
                {DatabaseTypeEnum.ByteArray, byte[].class},
                {DatabaseTypeEnum.SqlDate, Date.class},
                {DatabaseTypeEnum.SqlTime, Time.class},
                {DatabaseTypeEnum.SqlTimestamp, Timestamp.class}
        };

        for (int i = 0; i < types.length; i++) {
            DatabaseTypeEnum val = (DatabaseTypeEnum) types[i][0];
            assertEquals(types[i][1], val.getBinding().getType());
            assertEquals(types[i][1], val.getJavaClass());
        }
    }

    public void testLookup() {
        Object[][] types = new Object[][]{
                {"string", DatabaseTypeEnum.String},
                {"java.lang.string", DatabaseTypeEnum.String},
                {"java.lang.String", DatabaseTypeEnum.String},
                {"bigdecimal", DatabaseTypeEnum.BigDecimal},
                {Boolean.class.getName(), DatabaseTypeEnum.Boolean},
                {byte.class.getName(), DatabaseTypeEnum.Byte},
                {"short", DatabaseTypeEnum.Short},
                {"int", DatabaseTypeEnum.Int},
                {"java.lang.integer", DatabaseTypeEnum.Int},
                {int.class.getName(), DatabaseTypeEnum.Int},
                {Integer.class.getName(), DatabaseTypeEnum.Int},
                {"sqldate", DatabaseTypeEnum.SqlDate},
                {"date", DatabaseTypeEnum.SqlDate},
                {java.sql.Date.class.getName(), DatabaseTypeEnum.SqlDate},
                {java.sql.Time.class.getName(), DatabaseTypeEnum.SqlTime},
                {"time", DatabaseTypeEnum.SqlTime},
                {"sqltimestamp", DatabaseTypeEnum.SqlTimestamp},
                {"timestamp", DatabaseTypeEnum.SqlTimestamp}
        };

        for (int i = 0; i < types.length; i++) {
            DatabaseTypeEnum val = (DatabaseTypeEnum) types[i][1];
            assertEquals(val, DatabaseTypeEnum.getEnum((String) types[i][0]));
        }
    }
}
