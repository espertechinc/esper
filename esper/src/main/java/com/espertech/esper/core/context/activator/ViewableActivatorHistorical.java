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
package com.espertech.esper.core.context.activator;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.view.HistoricalEventViewable;

public class ViewableActivatorHistorical implements ViewableActivator {

    protected final HistoricalEventViewable historicalEventViewable;

    public ViewableActivatorHistorical(HistoricalEventViewable historicalEventViewable) {
        this.historicalEventViewable = historicalEventViewable;
    }

    public ViewableActivationResult activate(AgentInstanceContext agentInstanceContext, boolean isSubselect, boolean isRecoveringResilient) {
        return new ViewableActivationResult(historicalEventViewable, CollectionUtil.STOP_CALLBACK_NONE, null, null, null, false, false, null);
    }
}
