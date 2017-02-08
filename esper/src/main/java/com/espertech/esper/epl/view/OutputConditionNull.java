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
package com.espertech.esper.epl.view;

/**
 * An empty output condition that is always satisfied.
 */
public class OutputConditionNull extends OutputConditionBase implements OutputCondition {

    private static final boolean DO_OUTPUT = true;
    private static final boolean FORCE_UPDATE = false;

    /**
     * Ctor.
     *
     * @param outputCallback is the callback to make once the condition is satisfied
     */
    public OutputConditionNull(OutputCallback outputCallback) {
        super(outputCallback);
    }

    public void updateOutputCondition(int newEventsCount, int oldEventsCount) {
        outputCallback.continueOutputProcessing(DO_OUTPUT, FORCE_UPDATE);
    }

    public void terminated() {
        outputCallback.continueOutputProcessing(true, true);
    }

    public void stop() {
        // no action required
    }
}
