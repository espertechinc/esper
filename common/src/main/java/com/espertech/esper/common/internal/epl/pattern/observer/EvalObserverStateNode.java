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
package com.espertech.esper.common.internal.epl.pattern.observer;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.*;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;


/**
 * This class represents the state of an eventObserver sub-expression in the evaluation state tree.
 */
public class EvalObserverStateNode extends EvalStateNode implements ObserverEventEvaluator {
    protected final EvalObserverNode evalObserverNode;
    protected EventObserver eventObserver;

    /**
     * Constructor.
     *
     * @param parentNode       is the parent evaluator to call to indicate truth value
     * @param evalObserverNode is the factory node associated to the state
     */
    public EvalObserverStateNode(Evaluator parentNode,
                                 EvalObserverNode evalObserverNode) {
        super(parentNode);

        this.evalObserverNode = evalObserverNode;
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        if (PatternConsumptionUtil.containsEvent(matchEvent, eventObserver.getBeginState())) {
            quit();
            AgentInstanceContext agentInstanceContext = evalObserverNode.getContext().getAgentInstanceContext();
            agentInstanceContext.getAuditProvider().patternFalse(evalObserverNode.getFactoryNode(), this, agentInstanceContext);
            this.getParentEvaluator().evaluateFalse(this, true);
        }
    }

    @Override
    public EvalNode getFactoryNode() {
        return evalObserverNode;
    }

    @Override
    public PatternAgentInstanceContext getContext() {
        return evalObserverNode.getContext();
    }

    public void observerEvaluateTrue(MatchedEventMap matchEvent, boolean quitted) {
        AgentInstanceContext agentInstanceContext = evalObserverNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternObserverEvaluateTrue(evalObserverNode.factoryNode, matchEvent);
        agentInstanceContext.getAuditProvider().patternTrue(evalObserverNode.getFactoryNode(), this, matchEvent, quitted, agentInstanceContext);
        if (quitted) {
            agentInstanceContext.getAuditProvider().patternInstance(false, evalObserverNode.factoryNode, agentInstanceContext);
        }
        this.getParentEvaluator().evaluateTrue(matchEvent, this, quitted, null);
        agentInstanceContext.getInstrumentationProvider().aPatternObserverEvaluateTrue();
    }

    public void observerEvaluateFalse(boolean restartable) {
        AgentInstanceContext agentInstanceContext = evalObserverNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getAuditProvider().patternFalse(evalObserverNode.getFactoryNode(), this, agentInstanceContext);
        agentInstanceContext.getAuditProvider().patternInstance(false, evalObserverNode.factoryNode, agentInstanceContext);
        this.getParentEvaluator().evaluateFalse(this, restartable);
    }

    public void start(MatchedEventMap beginState) {
        AgentInstanceContext agentInstanceContext = evalObserverNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternObserverStart(evalObserverNode.factoryNode, beginState);
        agentInstanceContext.getAuditProvider().patternInstance(true, evalObserverNode.factoryNode, agentInstanceContext);

        eventObserver = evalObserverNode.getFactoryNode().getObserverFactory().makeObserver(getContext(), beginState, this, null, this.getParentEvaluator().isFilterChildNonQuitting());
        eventObserver.startObserve();

        agentInstanceContext.getInstrumentationProvider().aPatternObserverStart();
    }

    public final void quit() {
        AgentInstanceContext agentInstanceContext = evalObserverNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternObserverQuit(evalObserverNode.factoryNode);
        agentInstanceContext.getAuditProvider().patternInstance(false, evalObserverNode.factoryNode, agentInstanceContext);

        eventObserver.stopObserve();

        agentInstanceContext.getInstrumentationProvider().aPatternObserverQuit();
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitObserver(evalObserverNode.getFactoryNode(), this, eventObserver);
    }

    public boolean isNotOperator() {
        return false;
    }

    public boolean isFilterStateNode() {
        return false;
    }

    public boolean isObserverStateNodeNonRestarting() {
        return evalObserverNode.getFactoryNode().isObserverStateNodeNonRestarting();
    }

    public final String toString() {
        return "EvalObserverStateNode eventObserver=" + eventObserver;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalObserverStateNode.class);
}
