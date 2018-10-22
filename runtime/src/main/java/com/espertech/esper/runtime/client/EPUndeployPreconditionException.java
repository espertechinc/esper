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
package com.espertech.esper.runtime.client;

/**
 * Uneploy exception to indicate that a precondition is not satisfied
 */
public class EPUndeployPreconditionException extends EPUndeployException {
    /**
     * Ctor.
     *
     * @param message message
     */
    public EPUndeployPreconditionException(String message) {
        super("A precondition is not satisfied: " + message);
    }
}
