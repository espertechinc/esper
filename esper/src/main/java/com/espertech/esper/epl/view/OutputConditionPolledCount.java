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

import com.espertech.esper.epl.variable.VariableReader;
import com.espertech.esper.util.ExecutionPathDebugLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Output limit condition that is satisfied when either
 * the total number of new events arrived or the total number
 * of old events arrived is greater than a preset value.
 */
public final class OutputConditionPolledCount implements OutputConditionPolled {
    private final OutputConditionPolledCountFactory factory;
    private final OutputConditionPolledCountState state;
    private final VariableReader optionalVariableReader;

    public OutputConditionPolledCount(OutputConditionPolledCountFactory factory, OutputConditionPolledCountState state, VariableReader optionalVariableReader) {
        this.factory = factory;
        this.state = state;
        this.optionalVariableReader = optionalVariableReader;
    }

    public OutputConditionPolledCountState getState() {
        return state;
    }

    public final boolean updateOutputCondition(int newDataCount, int oldDataCount) {
        if (optionalVariableReader != null) {
            Object value = optionalVariableReader.getValue();
            if (value != null) {
                state.setEventRate(((Number) value).longValue());
            }
        }

        state.setNewEventsCount(state.getNewEventsCount() + newDataCount);
        state.setOldEventsCount(state.getOldEventsCount() + oldDataCount);

        if (isSatisfied() || state.isFirst()) {
            if ((ExecutionPathDebugLog.isDebugEnabled) && (log.isDebugEnabled())) {
                log.debug(".updateOutputCondition() condition satisfied");
            }
            state.setIsFirst(false);
            state.setNewEventsCount(0);
            state.setOldEventsCount(0);
            return true;
        }

        return false;
    }

    private boolean isSatisfied() {
        return (state.getNewEventsCount() >= state.getEventRate()) || (state.getOldEventsCount() >= state.getEventRate());
    }

    private static final Logger log = LoggerFactory.getLogger(OutputConditionPolledCount.class);
}
