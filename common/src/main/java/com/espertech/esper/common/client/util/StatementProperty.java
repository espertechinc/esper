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
package com.espertech.esper.common.client.util;

/**
 * Provides well-known statement properties.
 */
public enum StatementProperty {
    /**
     * The statement EPL text.
     */
    EPL,

    /**
     * The statement type
     */
    STATEMENTTYPE,

    /**
     * The name of the EPL-object created by the statement, or null if not applicable, i.e. the name of the
     * name window, table, variable, expression, index, schema or expression created by the statement.
     * <p>
     *     Use together with the statement type to determine the type of object.
     * </p>
     */
    CREATEOBJECTNAME
}
