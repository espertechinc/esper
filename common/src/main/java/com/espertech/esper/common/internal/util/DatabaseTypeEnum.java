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

import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.publicConstValue;

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
        bindings.put(String, DatabaseTypeBindingString.INSTANCE);
        bindings.put(BigDecimal, DatabaseTypeBindingBigDecimal.INSTANCE);
        bindings.put(Boolean, DatabaseTypeBindingBoolean.INSTANCE);
        bindings.put(Byte, DatabaseTypeBindingByte.INSTANCE);
        bindings.put(ByteArray, DatabaseTypeBindingByteArray.INSTANCE);
        bindings.put(Double, DatabaseTypeBindingDouble.INSTANCE);
        bindings.put(Float, DatabaseTypeBindingFloat.INSTANCE);
        bindings.put(Int, DatabaseTypeBindingInt.INSTANCE);
        bindings.put(Long, DatabaseTypeBindingLong.INSTANCE);
        bindings.put(Short, DatabaseTypeBindingShort.INSTANCE);
        bindings.put(SqlDate, DatabaseTypeBindingSqlDate.INSTANCE);
        bindings.put(SqlTime, DatabaseTypeBindingSqlTime.INSTANCE);
        bindings.put(SqlTimestamp, DatabaseTypeBindingSqlTimestamp.INSTANCE);
    }

    public static class DatabaseTypeBindingString implements DatabaseTypeBinding {
        public final static DatabaseTypeBindingString INSTANCE = new DatabaseTypeBindingString();

        private DatabaseTypeBindingString() {
        }

        public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
            return resultSet.getString(columnName);
        }

        public Class getType() {
            return String.class;
        }

        public CodegenExpression make() {
            return publicConstValue(this.getClass(), "INSTANCE");
        }
    }

    public static class DatabaseTypeBindingBigDecimal implements DatabaseTypeBinding {
        public final static DatabaseTypeBindingBigDecimal INSTANCE = new DatabaseTypeBindingBigDecimal();

        private DatabaseTypeBindingBigDecimal() {
        }

        public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
            return resultSet.getBigDecimal(columnName);
        }

        public Class getType() {
            return BigDecimal.class;
        }

        public CodegenExpression make() {
            return publicConstValue(this.getClass(), "INSTANCE");
        }
    }

    public static class DatabaseTypeBindingBoolean implements DatabaseTypeBinding {
        public final static DatabaseTypeBindingBoolean INSTANCE = new DatabaseTypeBindingBoolean();

        private DatabaseTypeBindingBoolean() {
        }

        public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
            return resultSet.getBoolean(columnName);
        }

        public Class getType() {
            return Boolean.class;
        }

        public CodegenExpression make() {
            return publicConstValue(this.getClass(), "INSTANCE");
        }
    }

    public static class DatabaseTypeBindingByte implements DatabaseTypeBinding {
        public final static DatabaseTypeBindingByte INSTANCE = new DatabaseTypeBindingByte();

        private DatabaseTypeBindingByte() {
        }

        public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
            return resultSet.getByte(columnName);
        }

        public Class getType() {
            return Byte.class;
        }

        public CodegenExpression make() {
            return publicConstValue(this.getClass(), "INSTANCE");
        }
    }

    public static class DatabaseTypeBindingByteArray implements DatabaseTypeBinding {
        public final static DatabaseTypeBindingByteArray INSTANCE = new DatabaseTypeBindingByteArray();

        private DatabaseTypeBindingByteArray() {
        }

        public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
            return resultSet.getBytes(columnName);
        }

        public Class getType() {
            return byte[].class;
        }

        public CodegenExpression make() {
            return publicConstValue(this.getClass(), "INSTANCE");
        }
    }

    public static class DatabaseTypeBindingDouble implements DatabaseTypeBinding {
        public final static DatabaseTypeBindingDouble INSTANCE = new DatabaseTypeBindingDouble();

        private DatabaseTypeBindingDouble() {
        }

        public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
            return resultSet.getDouble(columnName);
        }

        public Class getType() {
            return Double.class;
        }

        public CodegenExpression make() {
            return publicConstValue(this.getClass(), "INSTANCE");
        }
    }

    public static class DatabaseTypeBindingFloat implements DatabaseTypeBinding {
        public final static DatabaseTypeBindingFloat INSTANCE = new DatabaseTypeBindingFloat();

        private DatabaseTypeBindingFloat() {
        }

        public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
            return resultSet.getFloat(columnName);
        }

        public Class getType() {
            return Float.class;
        }

        public CodegenExpression make() {
            return publicConstValue(this.getClass(), "INSTANCE");
        }
    }

    public static class DatabaseTypeBindingInt implements DatabaseTypeBinding {
        public final static DatabaseTypeBindingInt INSTANCE = new DatabaseTypeBindingInt();

        private DatabaseTypeBindingInt() {
        }

        public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
            return resultSet.getInt(columnName);
        }

        public Class getType() {
            return Integer.class;
        }

        public CodegenExpression make() {
            return publicConstValue(this.getClass(), "INSTANCE");
        }
    }

    public static class DatabaseTypeBindingLong implements DatabaseTypeBinding {
        public final static DatabaseTypeBindingLong INSTANCE = new DatabaseTypeBindingLong();

        private DatabaseTypeBindingLong() {
        }

        public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
            return resultSet.getLong(columnName);
        }

        public Class getType() {
            return Long.class;
        }

        public CodegenExpression make() {
            return publicConstValue(this.getClass(), "INSTANCE");
        }
    }

    public static class DatabaseTypeBindingShort implements DatabaseTypeBinding {
        public final static DatabaseTypeBindingShort INSTANCE = new DatabaseTypeBindingShort();

        private DatabaseTypeBindingShort() {
        }

        public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
            return resultSet.getShort(columnName);
        }

        public Class getType() {
            return Short.class;
        }

        public CodegenExpression make() {
            return publicConstValue(this.getClass(), "INSTANCE");
        }
    }

    public static class DatabaseTypeBindingSqlDate implements DatabaseTypeBinding {
        public final static DatabaseTypeBindingSqlDate INSTANCE = new DatabaseTypeBindingSqlDate();

        private DatabaseTypeBindingSqlDate() {
        }

        public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
            return resultSet.getDate(columnName);
        }

        public Class getType() {
            return java.sql.Date.class;
        }

        public CodegenExpression make() {
            return publicConstValue(this.getClass(), "INSTANCE");
        }
    }

    public static class DatabaseTypeBindingSqlTime implements DatabaseTypeBinding {
        public final static DatabaseTypeBindingSqlTime INSTANCE = new DatabaseTypeBindingSqlTime();

        private DatabaseTypeBindingSqlTime() {
        }

        public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
            return resultSet.getTime(columnName);
        }

        public Class getType() {
            return java.sql.Time.class;
        }

        public CodegenExpression make() {
            return publicConstValue(this.getClass(), "INSTANCE");
        }
    }

    public static class DatabaseTypeBindingSqlTimestamp implements DatabaseTypeBinding {
        public final static DatabaseTypeBindingSqlTimestamp INSTANCE = new DatabaseTypeBindingSqlTimestamp();

        private DatabaseTypeBindingSqlTimestamp() {
        }

        public Object getValue(ResultSet resultSet, String columnName) throws SQLException {
            return resultSet.getTimestamp(columnName);
        }

        public Class getType() {
            return Timestamp.class;
        }

        public CodegenExpression make() {
            return publicConstValue(this.getClass(), "INSTANCE");
        }
    }
}
