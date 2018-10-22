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
package com.espertech.esper.common.internal.epl.pattern.followedby;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.*;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

import java.util.HashMap;
import java.util.Set;

/**
 * This class represents the state of a followed-by operator in the evaluation state tree.
 */
public class EvalFollowedByStateNode extends EvalStateNode implements Evaluator {
    protected final EvalFollowedByNode evalFollowedByNode;
    protected final HashMap<EvalStateNode, Integer> nodes;

    /**
     * Constructor.
     *
     * @param parentNode         is the parent evaluator to call to indicate truth value
     * @param evalFollowedByNode is the factory node associated to the state
     */
    public EvalFollowedByStateNode(Evaluator parentNode,
                                   EvalFollowedByNode evalFollowedByNode) {
        super(parentNode);

        this.evalFollowedByNode = evalFollowedByNode;
        this.nodes = new HashMap<EvalStateNode, Integer>();
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        PatternConsumptionUtil.childNodeRemoveMatches(matchEvent, nodes.keySet());
    }

    @Override
    public EvalNode getFactoryNode() {
        return evalFollowedByNode;
    }

    public final void start(MatchedEventMap beginState) {
        AgentInstanceContext agentInstanceContext = evalFollowedByNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternFollowedByStart(evalFollowedByNode.factoryNode, beginState);
        agentInstanceContext.getAuditProvider().patternInstance(true, evalFollowedByNode.factoryNode, agentInstanceContext);

        EvalNode child = evalFollowedByNode.getChildNodes()[0];
        EvalStateNode childState = child.newState(this);
        nodes.put(childState, 0);
        childState.start(beginState);

        agentInstanceContext.getInstrumentationProvider().aPatternFollowedByStart();
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        AgentInstanceContext agentInstanceContext = evalFollowedByNode.getContext().getAgentInstanceContext();
        Integer index = nodes.get(fromNode);
        agentInstanceContext.getInstrumentationProvider().qPatternFollowedByEvaluateTrue(evalFollowedByNode.factoryNode, matchEvent, index);

        if (isQuitted) {
            nodes.remove(fromNode);
        }

        // the node may already have quit as a result of an outer state quitting this state,
        // however the callback may still be received; It is fine to ignore this callback. 
        if (index == null) {
            agentInstanceContext.getInstrumentationProvider().aPatternFollowedByEvaluateTrue(false);
            return;
        }

        // If the match came from the very last filter, need to escalate
        int numChildNodes = evalFollowedByNode.getChildNodes().length;
        boolean isFollowedByQuitted = false;
        if (index == (numChildNodes - 1)) {

            if (nodes.isEmpty()) {
                isFollowedByQuitted = true;
                agentInstanceContext.getAuditProvider().patternInstance(false, evalFollowedByNode.factoryNode, agentInstanceContext);
            }

            agentInstanceContext.getAuditProvider().patternTrue(evalFollowedByNode.getFactoryNode(), this, matchEvent, isFollowedByQuitted, agentInstanceContext);
            this.getParentEvaluator().evaluateTrue(matchEvent, this, isFollowedByQuitted, optionalTriggeringEvent);
        } else {
            // Else start a new sub-expression for the next-in-line filter
            EvalNode child = evalFollowedByNode.getChildNodes()[index + 1];
            EvalStateNode childState = child.newState(this);
            nodes.put(childState, index + 1);
            childState.start(matchEvent);
        }

        agentInstanceContext.getInstrumentationProvider().aPatternFollowedByEvaluateTrue(isFollowedByQuitted);
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        AgentInstanceContext agentInstanceContext = evalFollowedByNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternFollowedByEvalFalse(evalFollowedByNode.factoryNode);

        fromNode.quit();
        nodes.remove(fromNode);

        if (nodes.isEmpty()) {
            agentInstanceContext.getAuditProvider().patternFalse(evalFollowedByNode.getFactoryNode(), this, agentInstanceContext);
            agentInstanceContext.getAuditProvider().patternInstance(false, evalFollowedByNode.factoryNode, agentInstanceContext);
            this.getParentEvaluator().evaluateFalse(this, true);
            quitInternal();
        }

        agentInstanceContext.getInstrumentationProvider().aPatternFollowedByEvalFalse();
    }

    public final void quit() {
        AgentInstanceContext agentInstanceContext = evalFollowedByNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternFollowedByQuit(evalFollowedByNode.factoryNode);
        agentInstanceContext.getAuditProvider().patternInstance(false, evalFollowedByNode.factoryNode, agentInstanceContext);

        if (nodes.isEmpty()) {
            agentInstanceContext.getInstrumentationProvider().aPatternFollowedByQuit();
            return;
        }
        quitInternal();

        agentInstanceContext.getInstrumentationProvider().aPatternFollowedByQuit();
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitFollowedBy(evalFollowedByNode.getFactoryNode(), this, nodes);
        for (EvalStateNode node : nodes.keySet()) {
            node.accept(visitor);
        }
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

    public final String toString() {
        return "EvalFollowedByStateNode nodes=" + nodes.size();
    }

    private final void quitInternal() {
        for (EvalStateNode child : nodes.keySet()) {
            child.quit();
        }
        nodes.clear();
    }
}
