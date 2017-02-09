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
package com.espertech.esper.epl.db;

import com.espertech.esper.util.DatabaseTypeBinding;

/**
 * Descriptor for SQL output columns.
 */
public class DBOutputTypeDesc {
    private int sqlType;
    private String className;
    private DatabaseTypeBinding optionalBinding;

    /**
     * Ctor.
     *
     * @param sqlType         the type of the column
     * @param className       the Java class reflecting column type
     * @param optionalBinding is the optional mapping from output column type to Java built-in
     */
    public DBOutputTypeDesc(int sqlType, String className, DatabaseTypeBinding optionalBinding) {
        this.sqlType = sqlType;
        this.className = className;
        this.optionalBinding = optionalBinding;
    }

    /**
     * Returns the SQL type of the output column.
     *
     * @return sql type
     */
    public int getSqlType() {
        return sqlType;
    }

    /**
     * Returns the class name that getObject() on the output column produces.
     *
     * @return class name from statement metadata
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns the optional mapping from output column type to Java built-in.
     *
     * @return database type mapping to Java type
     */
    public DatabaseTypeBinding getOptionalBinding() {
        return optionalBinding;
    }

    public String toString() {
        return "type=" + sqlType +
                " className=" + className;
    }
}


