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
     * The statement EPL text, of type {@link String}
     */
    EPL,

    /**
     * The statement type, of type {@link StatementType}
     */
    STATEMENTTYPE,

    /**
     * The name of the EPL-object created by the statement, of type {@link String}, or null if not applicable, i.e. the name of the
     * name window, table, variable, expression, index, schema or expression created by the statement.
     * <p>
     *     Use together with the statement type to determine the type of object.
     * </p>
     */
    CREATEOBJECTNAME,

    /**
     * The context name, of type {@link String}, or null if the statement is not associated to a context.
     */
    CONTEXTNAME,

    /**
     * The context deployment id, of type {@link String}, or null if the statement is not associated to a context.
     */
    CONTEXTDEPLOYMENTID
}
