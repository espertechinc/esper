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
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.table.core.Table;
import com.espertech.esper.common.internal.epl.table.core.TableInstance;
import com.espertech.esper.common.internal.epl.table.core.TableStateViewableInternal;

public class ViewableActivatorTable implements ViewableActivator {

    private Table table;
    private ExprEvaluator filterEval;

    public void setTable(Table table) {
        this.table = table;
    }

    public void setFilterEval(ExprEvaluator filterEval) {
        this.filterEval = filterEval;
    }

    public EventType getEventType() {
        return table.getMetaData().getPublicEventType();
    }

    public ViewableActivationResult activate(AgentInstanceContext agentInstanceContext, boolean isSubselect, boolean isRecoveringResilient) {
        TableInstance state = table.getTableInstance(agentInstanceContext.getAgentInstanceId());
        return new ViewableActivationResult(new TableStateViewableInternal(state, filterEval), AgentInstanceStopCallback.INSTANCE_NO_ACTION, null, false, false, null, null);
    }
}
