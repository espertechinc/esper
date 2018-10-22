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
package com.espertech.esper.common.internal.epl.pattern.filter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.EPStatementHandleCallbackFilter;
import com.espertech.esper.common.internal.epl.pattern.core.*;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import com.espertech.esper.common.internal.filtersvc.FilterHandleCallback;
import com.espertech.esper.common.internal.filtersvc.FilterService;

import java.util.Collection;
import java.util.Set;

/**
 * This class contains the state of a single filter expression in the evaluation state tree.
 */
public class EvalFilterStateNode extends EvalStateNode implements FilterHandleCallback {
    protected final EvalFilterNode evalFilterNode;

    protected boolean isStarted;
    protected EPStatementHandleCallbackFilter handle;
    protected MatchedEventMap beginState;

    /**
     * Constructor.
     *
     * @param parentNode     is the parent evaluator to call to indicate truth value
     * @param evalFilterNode is the factory node associated to the state
     */
    public EvalFilterStateNode(Evaluator parentNode,
                               EvalFilterNode evalFilterNode) {
        super(parentNode);
        this.evalFilterNode = evalFilterNode;
    }

    @Override
    public EvalNode getFactoryNode() {
        return evalFilterNode;
    }

    public final void start(MatchedEventMap beginState) {
        AgentInstanceContext agentInstanceContext = evalFilterNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternFilterStart(evalFilterNode.factoryNode, beginState);

        this.beginState = beginState;
        if (isStarted) {
            throw new IllegalStateException("Filter state node already active");
        }

        agentInstanceContext.getAuditProvider().patternInstance(true, evalFilterNode.factoryNode, agentInstanceContext);

        // Start the filter
        isStarted = true;

        FilterService filterService = evalFilterNode.getContext().getFilterService();
        handle = new EPStatementHandleCallbackFilter(evalFilterNode.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle(), this);
        FilterSpecActivatable filterSpec = evalFilterNode.getFactoryNode().getFilterSpec();
        FilterValueSetParam[][] filterValues = filterSpec.getValueSet(beginState, evalFilterNode.getAddendumFilters(), agentInstanceContext, agentInstanceContext.getStatementContextFilterEvalEnv());
        filterService.add(filterSpec.getFilterForEventType(), filterValues, handle);
        long filtersVersion = filterService.getFiltersVersion();
        evalFilterNode.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);

        agentInstanceContext.getInstrumentationProvider().aPatternFilterStart();
    }

    public final void quit() {
        AgentInstanceContext agentInstanceContext = evalFilterNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternFilterQuit(evalFilterNode.factoryNode, beginState);
        agentInstanceContext.getAuditProvider().patternInstance(false, evalFilterNode.factoryNode, agentInstanceContext);

        isStarted = false;
        stopFiltering();

        agentInstanceContext.getInstrumentationProvider().aPatternFilterQuit();
    }

    private void evaluateTrue(MatchedEventMap theEvent, boolean isQuitted, EventBean optionalTriggeringEvent) {
        AgentInstanceContext agentInstanceContext = evalFilterNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getAuditProvider().patternTrue(evalFilterNode.getFactoryNode(), this, theEvent, isQuitted, agentInstanceContext);
        this.getParentEvaluator().evaluateTrue(theEvent, this, isQuitted, optionalTriggeringEvent);
    }

    public EvalFilterNode getEvalFilterNode() {
        return evalFilterNode;
    }

    public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
        AgentInstanceContext agentInstanceContext = evalFilterNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternFilterMatch(evalFilterNode.factoryNode, theEvent);

        if (!isStarted) {
            agentInstanceContext.getInstrumentationProvider().aPatternFilterMatch(true);
            return;
        }

        MatchedEventMap passUp = beginState.shallowCopy();

        if (evalFilterNode.getFactoryNode().getFilterSpec().getOptionalPropertyEvaluator() != null) {
            EventBean[] propertyEvents = evalFilterNode.getFactoryNode().getFilterSpec().getOptionalPropertyEvaluator().getProperty(theEvent, evalFilterNode.getContext().getAgentInstanceContext());
            if (propertyEvents == null) {
                return; // no results, ignore match
            }
            // Add event itself to the match event structure if a tag was provided
            if (evalFilterNode.getFactoryNode().getEventAsName() != null) {
                passUp.add(evalFilterNode.getFactoryNode().getEventAsTagNumber(), propertyEvents);
            }
        } else {
            // Add event itself to the match event structure if a tag was provided
            if (evalFilterNode.getFactoryNode().getEventAsName() != null) {
                passUp.add(evalFilterNode.getFactoryNode().getEventAsTagNumber(), theEvent);
            }
        }

        // Explanation for the type cast...
        // Each state node stops listening if it resolves to true, and all nodes newState
        // new listeners again. However this would be a performance drain since
        // and expression such as "on all b()" would remove the listener for b() for every match
        // and the all node would newState a new listener. The remove operation and the add operation
        // therefore don't take place if the EvalEveryStateNode node sits on top of a EvalFilterStateNode node.
        boolean isQuitted = false;
        if (!(this.getParentEvaluator().isFilterChildNonQuitting())) {
            stopFiltering();
            isQuitted = true;
            agentInstanceContext.getAuditProvider().patternInstance(false, evalFilterNode.factoryNode, agentInstanceContext);
        }

        this.evaluateTrue(passUp, isQuitted, theEvent);

        agentInstanceContext.getInstrumentationProvider().aPatternFilterMatch(isQuitted);
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitFilter(evalFilterNode.getFactoryNode(), this, handle, beginState);
    }

    public boolean isSubSelect() {
        return false;
    }

    public final String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("EvalFilterStateNode");
        buffer.append(" tag=");
        buffer.append(evalFilterNode.getFactoryNode().getFilterSpec());
        buffer.append(" spec=");
        buffer.append(evalFilterNode.getFactoryNode().getFilterSpec());
        return buffer.toString();
    }

    public boolean isFilterStateNode() {
        return true;
    }

    public boolean isNotOperator() {
        return false;
    }

    public boolean isObserverStateNodeNonRestarting() {
        return false;
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        if (!isStarted) {
            return;
        }
        if (PatternConsumptionUtil.containsEvent(matchEvent, beginState)) {
            quit();
            AgentInstanceContext agentInstanceContext = evalFilterNode.getContext().getAgentInstanceContext();
            agentInstanceContext.getAuditProvider().patternFalse(evalFilterNode.getFactoryNode(), this, agentInstanceContext);
            this.getParentEvaluator().evaluateFalse(this, true);
        }
    }

    private void stopFiltering() {
        AgentInstanceContext agentInstanceContext = evalFilterNode.getContext().getAgentInstanceContext();
        FilterSpecActivatable filterSpec = evalFilterNode.getFactoryNode().getFilterSpec();
        FilterValueSetParam[][] filterValues = filterSpec.getValueSet(beginState, evalFilterNode.getAddendumFilters(), agentInstanceContext, agentInstanceContext.getStatementContextFilterEvalEnv());
        FilterService filterService = evalFilterNode.getContext().getFilterService();
        if (handle != null) {
            filterService.remove(handle, filterSpec.getFilterForEventType(), filterValues);
        }
        handle = null;
        isStarted = false;
        long filtersVersion = filterService.getFiltersVersion();
        evalFilterNode.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
    }
}
