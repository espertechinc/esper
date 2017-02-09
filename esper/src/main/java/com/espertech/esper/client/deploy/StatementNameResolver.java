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
package com.espertech.esper.client.deploy;

import java.io.Serializable;

/**
 * Implement this interface to provide a custom statement name for the statements deployed via the deployment API.
 * <p>
 * Statement names provided by the resolver override the statement name provided via the @Name annotation.
 * </p>
 */
public interface StatementNameResolver extends Serializable {
    /**
     * Returns the statement name to assign to a newly-deployed statement.
     * <p>
     * Implementations would typically interrogate the context object EPL expression
     * or module and module item information and determine the right statement name to assign.
     * </p>
     *
     * @param context the statement's deployment context
     * @return statement name or null if none needs to be assigned and the default or @Name annotated name should be used
     */
    public String getStatementName(StatementDeploymentContext context);
}
