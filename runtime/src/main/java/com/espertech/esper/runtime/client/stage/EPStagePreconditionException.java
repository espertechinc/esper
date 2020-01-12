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
package com.espertech.esper.runtime.client.stage;

/**
 * Indicated a precondition is violated for a stage or un-stage operation.
 */
public class EPStagePreconditionException extends EPStageException {

    /**
     * Ctor.
     * @param message message
     */
    public EPStagePreconditionException(String message) {
        super(message);
    }
}
