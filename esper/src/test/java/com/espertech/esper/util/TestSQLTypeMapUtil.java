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


import com.espertech.esper.client.util.ClassForNameProviderDefault;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public class TestSQLTypeMapUtil extends TestCase {
    public void testMapping() {
        Map<Integer, Class> testData = new HashMap<Integer, Class>();
        testData.put(Types.CHAR, String.class);
        testData.put(Types.VARCHAR, String.class);
        testData.put(Types.LONGVARCHAR, String.class);
        testData.put(Types.NUMERIC, BigDecimal.class);
        testData.put(Types.DECIMAL, BigDecimal.class);
        testData.put(Types.BIT, Boolean.class);
        testData.put(Types.BOOLEAN, Boolean.class);
        testData.put(Types.TINYINT, Byte.class);
        testData.put(Types.SMALLINT, Short.class);
        testData.put(Types.INTEGER, Integer.class);
        testData.put(Types.BIGINT, Long.class);
        testData.put(Types.REAL, Float.class);
        testData.put(Types.FLOAT, Double.class);
        testData.put(Types.DOUBLE, Double.class);
        testData.put(Types.BINARY, byte[].class);
        testData.put(Types.VARBINARY, byte[].class);
        testData.put(Types.LONGVARBINARY, byte[].class);
        testData.put(Types.DATE, java.sql.Date.class);
        testData.put(Types.TIMESTAMP, java.sql.Timestamp.class);
        testData.put(Types.TIME, java.sql.Time.class);
        testData.put(Types.CLOB, java.sql.Clob.class);
        testData.put(Types.BLOB, java.sql.Blob.class);
        testData.put(Types.ARRAY, java.sql.Array.class);
        testData.put(Types.STRUCT, java.sql.Struct.class);
        testData.put(Types.REF, java.sql.Ref.class);
        testData.put(Types.DATALINK, java.net.URL.class);

        for (int type : testData.keySet()) {
            Class result = SQLTypeMapUtil.sqlTypeToClass(type, null, ClassForNameProviderDefault.INSTANCE);
            log.debug(".testMapping Mapping " + type + " to " + result.getSimpleName());
            assertEquals(testData.get(type), result);
        }

        assertEquals(String.class, SQLTypeMapUtil.sqlTypeToClass(Types.JAVA_OBJECT, "java.lang.String", ClassForNameProviderDefault.INSTANCE));
        assertEquals(String.class, SQLTypeMapUtil.sqlTypeToClass(Types.DISTINCT, "java.lang.String", ClassForNameProviderDefault.INSTANCE));
    }

    public void testMappingInvalid() {
        tryInvalid(Types.JAVA_OBJECT, null);
        tryInvalid(Types.JAVA_OBJECT, "xx");
        tryInvalid(Types.DISTINCT, null);
        tryInvalid(Integer.MAX_VALUE, "yy");
    }

    private void tryInvalid(int type, String classname) {
        try {
            SQLTypeMapUtil.sqlTypeToClass(type, classname, ClassForNameProviderDefault.INSTANCE);
            fail();
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TestSQLTypeMapUtil.class);
}
