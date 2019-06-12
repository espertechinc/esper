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
package com.espertech.esper.common.internal.context.util;

/**
 * Thrown to indicate a precondition violation for undeploy.
 */
public class UndeployPreconditionException extends Exception {

    private static final long serialVersionUID = -4354386368238436441L;

    /**
     * Ctor.
     *
     * @param message - validation error message
     */
    public UndeployPreconditionException(String message) {
        super(message);
    }
}
