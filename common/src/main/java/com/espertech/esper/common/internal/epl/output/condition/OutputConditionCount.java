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
package com.espertech.esper.common.internal.epl.output.condition;

import com.espertech.esper.common.internal.epl.variable.core.VariableReader;

/**
 * Output limit condition that is satisfied when either
 * the total number of new events arrived or the total number
 * of old events arrived is greater than a preset value.
 */
public final class OutputConditionCount extends OutputConditionBase implements OutputCondition {
    private static final boolean DO_OUTPUT = true;
    private static final boolean FORCE_UPDATE = false;

    private long eventRate;
    private int newEventsCount;
    private int oldEventsCount;
    private VariableReader variableReader;

    public OutputConditionCount(OutputCallback outputCallback, long eventRate, VariableReader variableReader) {
        super(outputCallback);
        this.eventRate = eventRate;
        this.variableReader = variableReader;
    }

    /**
     * Returns the number of new events.
     *
     * @return number of new events
     */
    public int getNewEventsCount() {
        return newEventsCount;
    }

    /**
     * Returns the number of old events.
     *
     * @return number of old events
     */
    public int getOldEventsCount() {
        return oldEventsCount;
    }

    public final void updateOutputCondition(int newDataCount, int oldDataCount) {
        if (variableReader != null) {
            Object value = variableReader.getValue();
            if (value != null) {
                eventRate = ((Number) value).longValue();
            }
        }

        this.newEventsCount += newDataCount;
        this.oldEventsCount += oldDataCount;

        if (isSatisfied()) {
            this.newEventsCount = 0;
            this.oldEventsCount = 0;
            outputCallback.continueOutputProcessing(DO_OUTPUT, FORCE_UPDATE);
        }
    }

    public final String toString() {
        return this.getClass().getName() +
                " eventRate=" + eventRate;
    }

    private boolean isSatisfied() {
        return (newEventsCount >= eventRate) || (oldEventsCount >= eventRate);
    }

    public void terminated() {
        outputCallback.continueOutputProcessing(true, true);
    }

    public void stopOutputCondition() {
        // no action required
    }
}
