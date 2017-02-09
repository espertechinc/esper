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

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Binding from database output column type to Java object.
 */
public interface DatabaseTypeBinding extends Serializable {
    /**
     * Returns the Java object for the given column.
     *
     * @param resultSet  is the result set to read the column from
     * @param columnName is the column name
     * @return Java object
     * @throws SQLException if the mapping cannot be performed
     */
    public Object getValue(ResultSet resultSet, String columnName) throws SQLException;

    /**
     * Returns the Java target type.
     *
     * @return Java type
     */
    public Class getType();
}
