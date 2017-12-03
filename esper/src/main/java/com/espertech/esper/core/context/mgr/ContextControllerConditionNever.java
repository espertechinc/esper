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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.filterspec.MatchedEventMap;

/**
 * Context condition used for overlapping and non-overlaping to never-end/terminated.
 */
public class ContextControllerConditionNever implements ContextControllerCondition {

    public void activate(EventBean optionalTriggerEvent, MatchedEventMap priorMatches, long timeOffset, boolean isRecoveringResilient) {
    }

    public void deactivate() {
    }

    public boolean isRunning() {
        return true;
    }

    public Long getExpectedEndTime() {
        return null;
    }

    public boolean isImmediate() {
        return false;
    }
}
