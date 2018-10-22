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
package com.espertech.esper.common.internal.context.activator;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewable;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalEventViewableFactory;

public class ViewableActivatorHistorical implements ViewableActivator {
    private HistoricalEventViewableFactory factory;

    public void setFactory(HistoricalEventViewableFactory factory) {
        this.factory = factory;
    }

    public EventType getEventType() {
        return factory.getEventType();
    }

    public ViewableActivationResult activate(AgentInstanceContext agentInstanceContext, boolean isSubselect, boolean isRecoveringResilient) {
        HistoricalEventViewable viewable = factory.activate(agentInstanceContext);
        return new ViewableActivationResult(viewable, viewable, null, false, false, null, null);
    }
}
