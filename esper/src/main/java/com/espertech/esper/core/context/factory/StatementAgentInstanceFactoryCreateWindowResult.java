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
package com.espertech.esper.core.context.factory;

import com.espertech.esper.core.context.activator.ViewableActivationResult;
import com.espertech.esper.core.context.subselect.SubSelectStrategyHolder;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.prev.ExprPreviousEvalStrategy;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorEvalStrategy;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.table.ExprTableAccessEvalStrategy;
import com.espertech.esper.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.epl.named.NamedWindowProcessorInstance;
import com.espertech.esper.util.StopCallback;
import com.espertech.esper.view.Viewable;

import java.util.Collections;

public class StatementAgentInstanceFactoryCreateWindowResult extends StatementAgentInstanceFactoryResult {

    private final Viewable eventStreamParentViewable;
    private final StatementAgentInstancePostLoad postLoad;
    private final Viewable topView;
    private final NamedWindowProcessorInstance processorInstance;
    private final ViewableActivationResult viewableActivationResult;

    public StatementAgentInstanceFactoryCreateWindowResult(Viewable finalView, StopCallback stopCallback, AgentInstanceContext agentInstanceContext, Viewable eventStreamParentViewable, StatementAgentInstancePostLoad postLoad, Viewable topView, NamedWindowProcessorInstance processorInstance, ViewableActivationResult viewableActivationResult) {
        super(finalView, stopCallback, agentInstanceContext,
                null, Collections.<ExprSubselectNode, SubSelectStrategyHolder>emptyMap(),
                Collections.<ExprPriorNode, ExprPriorEvalStrategy>emptyMap(),
                Collections.<ExprPreviousNode, ExprPreviousEvalStrategy>emptyMap(),
                null,
                Collections.<ExprTableAccessNode, ExprTableAccessEvalStrategy>emptyMap(),
                Collections.<StatementAgentInstancePreload>emptyList()
        );
        this.eventStreamParentViewable = eventStreamParentViewable;
        this.postLoad = postLoad;
        this.topView = topView;
        this.processorInstance = processorInstance;
        this.viewableActivationResult = viewableActivationResult;
    }

    public Viewable getEventStreamParentViewable() {
        return eventStreamParentViewable;
    }

    public StatementAgentInstancePostLoad getPostLoad() {
        return postLoad;
    }

    public Viewable getTopView() {
        return topView;
    }

    public NamedWindowProcessorInstance getProcessorInstance() {
        return processorInstance;
    }

    public ViewableActivationResult getViewableActivationResult() {
        return viewableActivationResult;
    }
}
