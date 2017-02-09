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
 * Implement this interface to provide a custom user object for the statements deployed via the deployment API.
 */
public interface StatementUserObjectResolver extends Serializable {
    /**
     * Returns the user object to assign to a newly-deployed statement.
     * <p>
     * Implementations would typically interrogate the context object EPL expression
     * or module and module item information and determine the right user object to assign.
     * </p>
     *
     * @param context the statement's deployment context
     * @return user object or null if none needs to be assigned
     */
    public Object getUserObject(StatementDeploymentContext context);
}
