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
 * A condition that must be satisfied before output processing
 * is allowed to continue. Once the condition is satisfied, it
 * makes a callback to continue output processing.
 */
public interface OutputCondition {
    /**
     * Update the output condition.
     *
     * @param newEventsCount - number of new events incoming
     * @param oldEventsCount - number of old events incoming
     */
    void updateOutputCondition(int newEventsCount, int oldEventsCount);

    void terminated();

    void stop();
}
