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
package com.espertech.esper.common.internal.epl.pattern.guard;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.*;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

import java.util.Set;

/**
 * This class represents the state of a "within" operator in the evaluation state tree.
 * The within operator applies to a subexpression and is thus expected to only
 * have one child node.
 */
public class EvalGuardStateNode extends EvalStateNode implements Evaluator, Quitable {
    protected EvalGuardNode evalGuardNode;
    protected EvalStateNode activeChildNode;
    protected Guard guard;
    protected MatchedEventMap beginState;

    /**
     * Constructor.
     *
     * @param parentNode    is the parent evaluator to call to indicate truth value
     * @param evalGuardNode is the factory node associated to the state
     */
    public EvalGuardStateNode(Evaluator parentNode,
                              EvalGuardNode evalGuardNode) {
        super(parentNode);
        this.evalGuardNode = evalGuardNode;
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        if (PatternConsumptionUtil.containsEvent(matchEvent, beginState)) {
            quit();
            AgentInstanceContext agentInstanceContext = evalGuardNode.getContext().getAgentInstanceContext();
            agentInstanceContext.getAuditProvider().patternFalse(evalGuardNode.getFactoryNode(), this, agentInstanceContext);
            this.getParentEvaluator().evaluateFalse(this, true);
        } else {
            if (activeChildNode != null) {
                activeChildNode.removeMatch(matchEvent);
            }
        }
    }

    @Override
    public EvalNode getFactoryNode() {
        return evalGuardNode;
    }

    public PatternAgentInstanceContext getContext() {
        return evalGuardNode.getContext();
    }

    public void start(MatchedEventMap beginState) {
        AgentInstanceContext agentInstanceContext = evalGuardNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternGuardStart(evalGuardNode.factoryNode, beginState);
        agentInstanceContext.getAuditProvider().patternInstance(true, evalGuardNode.factoryNode, agentInstanceContext);

        this.beginState = beginState;
        guard = evalGuardNode.getFactoryNode().getGuardFactory().makeGuard(evalGuardNode.getContext(), beginState, this, null);
        activeChildNode = evalGuardNode.getChildNode().newState(this);

        // Start the single child state
        activeChildNode.start(beginState);

        // Start the guard
        guard.startGuard();

        agentInstanceContext.getInstrumentationProvider().aPatternGuardStart();
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        AgentInstanceContext agentInstanceContext = evalGuardNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternGuardEvaluateTrue(evalGuardNode.factoryNode, matchEvent);

        boolean haveQuitted = activeChildNode == null;

        // If one of the children quits, remove the child
        if (isQuitted) {
            agentInstanceContext.getAuditProvider().patternInstance(false, evalGuardNode.factoryNode, agentInstanceContext);
            activeChildNode = null;

            // Stop guard, since associated subexpression is gone
            guard.stopGuard();
        }

        if (!haveQuitted) {
            boolean guardPass = guard.inspect(matchEvent);
            if (guardPass) {
                agentInstanceContext.getAuditProvider().patternTrue(evalGuardNode.getFactoryNode(), this, matchEvent, isQuitted, agentInstanceContext);
                this.getParentEvaluator().evaluateTrue(matchEvent, this, isQuitted, optionalTriggeringEvent);
            }
        }

        agentInstanceContext.getInstrumentationProvider().aPatternGuardEvaluateTrue(isQuitted);
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        activeChildNode = null;
        AgentInstanceContext agentInstanceContext = evalGuardNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternGuardEvalFalse(evalGuardNode.factoryNode);
        agentInstanceContext.getAuditProvider().patternFalse(evalGuardNode.getFactoryNode(), this, agentInstanceContext);
        agentInstanceContext.getAuditProvider().patternInstance(false, evalGuardNode.factoryNode, agentInstanceContext);
        this.getParentEvaluator().evaluateFalse(this, true);
        agentInstanceContext.getInstrumentationProvider().aPatternGuardEvalFalse();
    }

    public final void quit() {
        if (activeChildNode == null) {
            return;
        }

        AgentInstanceContext agentInstanceContext = evalGuardNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternGuardQuit(evalGuardNode.factoryNode);
        agentInstanceContext.getAuditProvider().patternInstance(false, evalGuardNode.factoryNode, agentInstanceContext);

        if (activeChildNode != null) {
            activeChildNode.quit();
            guard.stopGuard();
        }

        activeChildNode = null;

        agentInstanceContext.getInstrumentationProvider().aPatternGuardQuit();
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitGuard(evalGuardNode.getFactoryNode(), this, guard);
        if (activeChildNode != null) {
            activeChildNode.accept(visitor);
        }
    }

    public final String toString() {
        return "EvaluationWitinStateNode activeChildNode=" + activeChildNode +
                " guard=" + guard;
    }

    public boolean isNotOperator() {
        return false;
    }

    public boolean isFilterStateNode() {
        return false;
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isObserverStateNodeNonRestarting() {
        return false;
    }

    public void guardQuit() {
        AgentInstanceContext agentInstanceContext = evalGuardNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternGuardGuardQuit(evalGuardNode.factoryNode);

        // It is possible that the child node has already been quit such as when the parent wait time was shorter.
        // 1. parent node's guard indicates quit to all children
        // 2. this node's guards also indicates quit, however that already occured
        if (activeChildNode != null) {
            activeChildNode.quit();
        }
        activeChildNode = null;

        // Indicate to parent state that this is permanently false.
        agentInstanceContext.getAuditProvider().patternFalse(evalGuardNode.getFactoryNode(), this, agentInstanceContext);
        agentInstanceContext.getAuditProvider().patternInstance(false, evalGuardNode.factoryNode, agentInstanceContext);
        this.getParentEvaluator().evaluateFalse(this, true);

        agentInstanceContext.getInstrumentationProvider().aPatternGuardGuardQuit();
    }
}
