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

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Enumeration of the different Java built-in types that are used to represent database output column values.
 * <p>
 * Assigns a name to each type that serves as a short name in mapping, and a Java class type.
 * <p>
 * Provides binding implementations that use the correct ResultSet.get method to pull the correct type
 * out of a statement's result set.
 */
public enum DatabaseTypeEnum {
    /**
     * String type.
     */
    String(String.class),

    /**
     * Big decimal.
     */
    BigDecimal(BigDecimal.class),

    /**
     * Boolean type.
     */
    Boolean(Boolean.class),

    /**
     * Byte type.
     */
    Byte(Byte.class),

    /**
     * Short type.
     */
    Short(Short.class),

    /**
     * Integer type.
     */
    Int(Integer.class),

    /**
     * Long type.
     */
    Long(Long.class),

    /**
     * Float type.
     */
    Float(Float.class),

    /**
     * Double type.
     */
    Double(Double.class),

    /**
     * Byte array type.
     */
    ByteArray(byte[].class),

    /**
     * SQL Date type.
     */
    SqlDate(Date.class),

    /**
     * SQL time type.
     */
    SqlTime(Time.class),

    /**
     * SQL timestamp type.
     */
    SqlTimestamp(Timestamp.class);

    private Class javaClass;

    private DatabaseTypeEnum(Class javaClass) {
        this.javaClass = javaClass;
    }

    /**
     * Retuns the Java class for the name.
     *
     * @return Java class
     */
    public Class getJavaClass() {
        return javaClass;
    }

    /**
     * Given a type name, matches for simple and fully-qualified Java class name (case-insensitive)
     * as well as case-insensitive type name.
     *
     * @param type is the named type
     * @return type enumeration value for type
     */
    public static DatabaseTypeEnum getEnum(String type) {
        String boxedType = JavaClassHelper.getBoxedClassName(type);
        for (DatabaseTypeEnum val : DatabaseTypeEnum.values()) {
            if (val.toString().toLowerCase(Locale.ENGLISH).equals(type.toLowerCase(Locale.ENGLISH))) {
                return val;
            }
            if (val.getJavaClass().getName().toLowerCase(Locale.ENGLISH).equals(type.toLowerCase(Locale.ENGLISH))) {
                return val;
            }
            if (val.getJavaClass().getName().toLowerCase(Locale.ENGLISH).equals(boxedType)) {
                return val;
            }
            if (val.getJavaClass().getSimpleName().toLowerCase(Locale.ENGLISH).equals(boxedType)) {
                return val;
            }
        }
        return null;
    }

    /**
     * Returns the binding for this enumeration value for
     * reading the database result set and returning the right Java type.
     *
     * @return mapping of output column type to Java built-in
     */
    public DatabaseTypeBinding getBinding() {
        return bindings.get(this);
    }

    private static Map<DatabaseTypeEnum, DatabaseTypeBinding> bindings;

    static {
        bindings = new HashMap<DatabaseTypeEnum, DatabaseTypeBinding>();

        bindings.put(String, new DatabaseTypeBinding() {
            private static final long serialVersionUID = -5155039606411608898L;

            public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
                return resultSet.getString(columnName);
            }

            public Class getType() {
                return String.class;
            }
        });

        bindings.put(BigDecimal, new DatabaseTypeBinding() {
            private static final long serialVersionUID = -648825691097776732L;

            public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
                return resultSet.getBigDecimal(columnName);
            }

            public Class getType() {
                return BigDecimal.class;
            }
        });

        bindings.put(Boolean, new DatabaseTypeBinding() {
            private static final long serialVersionUID = 5760469994325386982L;

            public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
                return resultSet.getBoolean(columnName);
            }

            public Class getType() {
                return Boolean.class;
            }
        });

        bindings.put(Byte, new DatabaseTypeBinding() {
            private static final long serialVersionUID = -2840004368317980229L;

            public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
                return resultSet.getByte(columnName);
            }

            public Class getType() {
                return Byte.class;
            }
        });

        bindings.put(ByteArray, new DatabaseTypeBinding() {
            private static final long serialVersionUID = 7531500030996242544L;

            public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
                return resultSet.getBytes(columnName);
            }

            public Class getType() {
                return byte[].class;
            }
        });

        bindings.put(Double, new DatabaseTypeBinding() {
            private static final long serialVersionUID = -7421693646427315376L;

            public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
                return resultSet.getDouble(columnName);
            }

            public Class getType() {
                return Double.class;
            }
        });

        bindings.put(Float, new DatabaseTypeBinding() {
            private static final long serialVersionUID = -3368945008278992912L;

            public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
                return resultSet.getFloat(columnName);
            }

            public Class getType() {
                return Float.class;
            }
        });

        bindings.put(Int, new DatabaseTypeBinding() {
            private static final long serialVersionUID = 84694877489363201L;

            public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
                return resultSet.getInt(columnName);
            }

            public Class getType() {
                return Integer.class;
            }
        });

        bindings.put(Long, new DatabaseTypeBinding() {
            private static final long serialVersionUID = -1021242584650775564L;

            public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
                return resultSet.getLong(columnName);
            }

            public Class getType() {
                return Long.class;
            }
        });

        bindings.put(Short, new DatabaseTypeBinding() {
            private static final long serialVersionUID = -2922908857447642497L;

            public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
                return resultSet.getShort(columnName);
            }

            public Class getType() {
                return Short.class;
            }
        });

        bindings.put(SqlDate, new DatabaseTypeBinding() {
            private static final long serialVersionUID = 7869800941910757215L;

            public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
                return resultSet.getDate(columnName);
            }

            public Class getType() {
                return java.sql.Date.class;
            }
        });

        bindings.put(SqlTime, new DatabaseTypeBinding() {
            private static final long serialVersionUID = -5259494991875064798L;

            public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
                return resultSet.getTime(columnName);
            }

            public Class getType() {
                return java.sql.Time.class;
            }
        });

        bindings.put(SqlTimestamp, new DatabaseTypeBinding() {
            private static final long serialVersionUID = -8708251362439431175L;

            public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
                return resultSet.getTimestamp(columnName);
            }

            public Class getType() {
                return Timestamp.class;
            }
        });
    }
}
