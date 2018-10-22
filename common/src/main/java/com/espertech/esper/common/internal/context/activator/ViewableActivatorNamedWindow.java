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
import com.espertech.esper.common.internal.context.aifactory.core.ModuleIncidentals;
import com.espertech.esper.common.internal.context.module.StatementReadyCallback;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.context.util.StatementFinalizeCallback;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerDesc;
import com.espertech.esper.common.internal.epl.namedwindow.consume.NamedWindowConsumerView;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindow;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowWDirectConsume;
import com.espertech.esper.common.internal.filterspec.PropertyEvaluator;

public class ViewableActivatorNamedWindow implements ViewableActivator, StatementReadyCallback {

    protected ExprEvaluator filterEvaluator;
    protected QueryGraph filterQueryGraph;
    protected int namedWindowConsumerId;
    protected boolean subquery;
    protected NamedWindow namedWindow;
    protected PropertyEvaluator optPropertyEvaluator;

    public void setNamedWindowConsumerId(int namedWindowConsumerId) {
        this.namedWindowConsumerId = namedWindowConsumerId;
    }

    public void setNamedWindow(NamedWindow namedWindow) {
        this.namedWindow = namedWindow;
    }

    public void setFilterEvaluator(ExprEvaluator filterEvaluator) {
        this.filterEvaluator = filterEvaluator;
    }

    public void setSubquery(boolean subquery) {
        this.subquery = subquery;
    }

    public QueryGraph getFilterQueryGraph() {
        return filterQueryGraph;
    }

    public void setFilterQueryGraph(QueryGraph filterQueryGraph) {
        this.filterQueryGraph = filterQueryGraph;
    }

    public void setOptPropertyEvaluator(PropertyEvaluator optPropertyEvaluator) {
        this.optPropertyEvaluator = optPropertyEvaluator;
    }

    public EventType getEventType() {
        return namedWindow.getRootView().getEventType();
    }

    public void ready(StatementContext statementContext, ModuleIncidentals moduleIncidentals, boolean recovery) {
        String namedWindowName = namedWindow.getName();
        String namedWindowDeploymentId = namedWindow.getStatementContext().getDeploymentId();

        statementContext.getNamedWindowConsumerManagementService().addConsumer(namedWindowDeploymentId, namedWindowName, namedWindowConsumerId, statementContext, subquery);

        statementContext.addFinalizeCallback(new StatementFinalizeCallback() {
            public void statementDestroyed(StatementContext context) {
                statementContext.getNamedWindowConsumerManagementService().destroyConsumer(namedWindowDeploymentId, namedWindowName, context);
            }
        });
    }

    public ViewableActivationResult activate(AgentInstanceContext agentInstanceContext, boolean isSubselect, boolean isRecoveringResilient) {
        NamedWindowWDirectConsume nw = (NamedWindowWDirectConsume) namedWindow;
        NamedWindowConsumerDesc consumerDesc = new NamedWindowConsumerDesc(namedWindowConsumerId, filterEvaluator, optPropertyEvaluator, agentInstanceContext);
        NamedWindowConsumerView consumerView = nw.addConsumer(consumerDesc, isSubselect);
        return new ViewableActivationResult(consumerView, consumerView, null, false, false, null, null);
    }

    public String getNamedWindowContextName() {
        return namedWindow.getStatementContext().getContextName();
    }
}
