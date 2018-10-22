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
package com.espertech.esper.runtime.client.option;

/**
 * Implement this interface to provide a statement name at runtime for statements when they are deployed.
 */
public interface StatementNameRuntimeOption {
    /**
     * Returns the statement name to assign to a newly-deployed statement.
     * <p>
     * Implementations would typically interrogate the context object EPL expression
     * or module and module item information and determine the right statement name to assign.
     * </p>
     * <p>
     * When using HA the returned object must implement the Serializable interface.
     * </p>
     *
     * @param env the statement's deployment context
     * @return statement name or null if none needs to be assigned
     */
    public String getStatementName(StatementNameRuntimeContext env);
}
