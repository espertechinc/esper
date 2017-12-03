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
package com.espertech.esper.pattern;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.EPStatementHandleCallback;
import com.espertech.esper.filter.FilterHandleCallback;
import com.espertech.esper.filter.FilterService;
import com.espertech.esper.filter.FilterServiceEntry;
import com.espertech.esper.filterspec.FilterValueSet;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * This class contains the state of a single filter expression in the evaluation state tree.
 */
public class EvalFilterStateNode extends EvalStateNode implements FilterHandleCallback {
    protected final EvalFilterNode evalFilterNode;

    protected boolean isStarted;
    protected EPStatementHandleCallback handle;
    protected FilterServiceEntry filterServiceEntry;
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

    public int getStatementId() {
        return evalFilterNode.getContext().getPatternContext().getStatementId();
    }

    public final void start(MatchedEventMap beginState) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternFilterStart(evalFilterNode, beginState);
        }
        this.beginState = beginState;
        if (isStarted) {
            throw new IllegalStateException("Filter state node already active");
        }

        // Start the filter
        isStarted = true;
        startFiltering();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternFilterStart();
        }
    }

    public final void quit() {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternFilterQuit(evalFilterNode, beginState);
        }
        isStarted = false;
        stopFiltering();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternFilterQuit();
        }
    }

    private void evaluateTrue(MatchedEventMap theEvent, boolean isQuitted, EventBean optionalTriggeringEvent) {
        this.getParentEvaluator().evaluateTrue(theEvent, this, isQuitted, optionalTriggeringEvent);
    }

    public EvalFilterNode getEvalFilterNode() {
        return evalFilterNode;
    }

    public void matchFound(EventBean theEvent, Collection<FilterHandleCallback> allStmtMatches) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternFilterMatch(evalFilterNode, theEvent);
        }

        if (!isStarted) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aPatternFilterMatch(true);
            }
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
        }

        this.evaluateTrue(passUp, isQuitted, theEvent);

        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternFilterMatch(isQuitted);
        }
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
            this.getParentEvaluator().evaluateFalse(this, true);
        }
    }

    protected void startFiltering() {
        FilterService filterService = evalFilterNode.getContext().getPatternContext().getFilterService();
        handle = new EPStatementHandleCallback(evalFilterNode.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle(), this);
        AgentInstanceContext agentInstanceContext = evalFilterNode.getContext().getAgentInstanceContext();
        FilterValueSet filterValues = evalFilterNode.getFactoryNode().getFilterSpec().getValueSet(beginState, evalFilterNode.getAddendumFilters(), agentInstanceContext, agentInstanceContext.getEngineImportService(), agentInstanceContext.getAnnotations());
        filterServiceEntry = filterService.add(filterValues, handle);
        long filtersVersion = filterService.getFiltersVersion();
        evalFilterNode.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
    }

    private void stopFiltering() {
        PatternContext context = evalFilterNode.getContext().getPatternContext();
        if (handle != null) {
            context.getFilterService().remove(handle, filterServiceEntry);
        }
        handle = null;
        filterServiceEntry = null;
        isStarted = false;
        long filtersVersion = context.getFilterService().getFiltersVersion();
        evalFilterNode.getContext().getAgentInstanceContext().getEpStatementAgentInstanceHandle().getStatementFilterVersion().setStmtFilterVersion(filtersVersion);
    }

    private static final Logger log = LoggerFactory.getLogger(EvalFilterStateNode.class);
}
