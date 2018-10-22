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
package com.espertech.esper.common.internal.epl.ontrigger;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.OneEventCollection;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;

public class InfraOnMergeActionDel extends InfraOnMergeAction {

    public InfraOnMergeActionDel(ExprEvaluator optionalFilter) {
        super(optionalFilter);
    }

    public void apply(EventBean matchingEvent, EventBean[] eventsPerStream, OneEventCollection newData, OneEventCollection oldData, AgentInstanceContext agentInstanceContext) {
        oldData.add(matchingEvent);
    }

    public void apply(EventBean matchingEvent, EventBean[] eventsPerStream, TableInstance tableStateInstance, OnExprViewTableChangeHandler changeHandlerAdded, OnExprViewTableChangeHandler changeHandlerRemoved, AgentInstanceContext agentInstanceContext) {
        tableStateInstance.deleteEvent(matchingEvent);
        if (changeHandlerRemoved != null) {
            changeHandlerRemoved.add(matchingEvent, eventsPerStream, false, agentInstanceContext);
        }
    }

    public String getName() {
        return "delete";
    }
}
