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
package com.espertech.esper.common.internal.util;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.client.util.ClassForNameProvider;

import java.sql.Types;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility for mapping SQL types of {@link java.sql.Types} to Java classes.
 */
public class SQLTypeMapUtil {
    private static Map<String, Integer> sqlTypeMap;

    static {
        sqlTypeMap = new HashMap<String, Integer>();
        sqlTypeMap.put("BIT", Types.BIT);
        sqlTypeMap.put("TINYINT", Types.TINYINT);
        sqlTypeMap.put("SMALLINT", Types.SMALLINT);
        sqlTypeMap.put("INTEGER", Types.INTEGER);
        sqlTypeMap.put("BIGINT", Types.BIGINT);
        sqlTypeMap.put("FLOAT", Types.FLOAT);
        sqlTypeMap.put("REAL", Types.REAL);
        sqlTypeMap.put("DOUBLE", Types.DOUBLE);
        sqlTypeMap.put("NUMERIC", Types.NUMERIC);
        sqlTypeMap.put("DECIMAL", Types.DECIMAL);
        sqlTypeMap.put("CHAR", Types.CHAR);
        sqlTypeMap.put("VARCHAR", Types.VARCHAR);
        sqlTypeMap.put("LONGVARCHAR", Types.LONGVARCHAR);
        sqlTypeMap.put("DATE", Types.DATE);
        sqlTypeMap.put("TIME", Types.TIME);
        sqlTypeMap.put("TIMESTAMP", Types.TIMESTAMP);
        sqlTypeMap.put("BINARY", Types.BINARY);
        sqlTypeMap.put("VARBINARY", Types.VARBINARY);
        sqlTypeMap.put("LONGVARBINARY", Types.LONGVARBINARY);
        sqlTypeMap.put("NULL", Types.NULL);
        sqlTypeMap.put("OTHER", Types.OTHER);
        sqlTypeMap.put("JAVA_OBJECT", Types.JAVA_OBJECT);
        sqlTypeMap.put("DISTINCT", Types.DISTINCT);
        sqlTypeMap.put("STRUCT", Types.STRUCT);
        sqlTypeMap.put("ARRAY", Types.ARRAY);
        sqlTypeMap.put("BLOB", Types.BLOB);
        sqlTypeMap.put("CLOB", Types.CLOB);
        sqlTypeMap.put("REF", Types.REF);
        sqlTypeMap.put("DATALINK", Types.DATALINK);
        sqlTypeMap.put("BOOLEAN", Types.BOOLEAN);
    }

    /**
     * Mapping as defined by JDBC 3 Spec , page B-177, table B-1 JBDC Types mapped to Java Types.
     *
     * @param sqlType              to return Java class for
     * @param className            is the classname that result metadata returns for a column
     * @param classForNameProvider class-for-classname lookup
     * @return Java class for JDBC sql types
     */
    public static EPTypeClass sqlTypeToClass(int sqlType, String className, ClassForNameProvider classForNameProvider) {
        if ((sqlType == Types.BOOLEAN) ||
                (sqlType == Types.BIT)) {
            return EPTypePremade.BOOLEANBOXED.getEPType();
        }
        if ((sqlType == Types.CHAR) ||
                (sqlType == Types.VARCHAR) ||
                (sqlType == Types.LONGVARCHAR)) {
            return EPTypePremade.STRING.getEPType();
        }
        if ((sqlType == Types.DOUBLE) ||
                (sqlType == Types.FLOAT)) {
            return EPTypePremade.DOUBLEBOXED.getEPType();
        }
        if (sqlType == Types.REAL) {
            return EPTypePremade.FLOATBOXED.getEPType();
        }
        if (sqlType == Types.INTEGER) {
            return EPTypePremade.INTEGERBOXED.getEPType();
        }
        if (sqlType == Types.BIGINT) {
            return EPTypePremade.LONGBOXED.getEPType();
        }
        if (sqlType == Types.TINYINT) {
            return EPTypePremade.BYTEBOXED.getEPType();
        }
        if (sqlType == Types.SMALLINT) {
            return EPTypePremade.SHORTBOXED.getEPType();
        }
        if ((sqlType == Types.NUMERIC) ||
                (sqlType == Types.DECIMAL)) {
            return EPTypePremade.BIGDECIMAL.getEPType();
        }
        if ((sqlType == Types.BINARY) ||
                (sqlType == Types.VARBINARY) ||
                (sqlType == Types.LONGVARBINARY)) {
            return EPTypePremade.BYTEPRIMITIVEARRAY.getEPType();
        }
        if (sqlType == Types.DATE) {
            return EPTypePremade.SQLDATE.getEPType();
        }
        if (sqlType == Types.TIME) {
            return EPTypePremade.SQLTIME.getEPType();
        }
        if (sqlType == Types.TIMESTAMP) {
            return EPTypePremade.SQLTIMESTAMP.getEPType();
        }
        if (sqlType == Types.CLOB) {
            return EPTypePremade.SQLCLOB.getEPType();
        }
        if (sqlType == Types.BLOB) {
            return EPTypePremade.SQLBLOB.getEPType();
        }
        if (sqlType == Types.ARRAY) {
            return EPTypePremade.SQLARRAY.getEPType();
        }
        if (sqlType == Types.STRUCT) {
            return EPTypePremade.SQLSTRUCT.getEPType();
        }
        if (sqlType == Types.REF) {
            return EPTypePremade.SQLREF.getEPType();
        }
        if (sqlType == Types.DATALINK) {
            return EPTypePremade.NETURL.getEPType();
        }
        if ((sqlType == Types.JAVA_OBJECT) ||
                (sqlType == Types.DISTINCT)) {
            if (className == null) {
                throw new IllegalArgumentException("No class supplied for sql type " + sqlType);
            }
            try {
                Class clazz = classForNameProvider.classForName(className);
                return ClassHelperGenericType.getClassEPType(clazz);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Cannot load class for sql type " + sqlType + " and class " + className);
            }
        }
        throw new IllegalArgumentException("Cannot map java.sql.Types type " + sqlType);
    }

    /**
     * Returns the SQL type by type name.
     *
     * @param type sql type name
     * @return type sql type
     */
    public static int getSQLTypeByName(String type) {
        Integer val = sqlTypeMap.get(type.toUpperCase(Locale.ENGLISH));
        if (val != null) {
            return val;
        }
        throw new RuntimeException("Type by name '" + type + "' is not a recognized java.sql.Types type");
    }
}
