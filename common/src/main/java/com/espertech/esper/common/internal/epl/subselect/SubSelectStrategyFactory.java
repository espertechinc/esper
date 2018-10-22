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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.common.internal.view.core.Viewable;

import java.util.List;

public interface SubSelectStrategyFactory {
    void ready(StatementContext statementContext, EventType eventType);

    SubSelectStrategyRealization instantiate(Viewable viewableRoot, AgentInstanceContext agentInstanceContext, List<AgentInstanceStopCallback> stopCallbackList, int subqueryNumber, boolean isRecoveringResilient);

    LookupStrategyDesc getLookupStrategyDesc();
}
