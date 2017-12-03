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

public interface ContextControllerCondition {

    public void activate(EventBean optionalTriggeringEvent, MatchedEventMap priorMatches, long timeOffset, boolean isRecoveringReslient);

    public void deactivate();

    public boolean isRunning();

    public Long getExpectedEndTime();

    public boolean isImmediate();
}
