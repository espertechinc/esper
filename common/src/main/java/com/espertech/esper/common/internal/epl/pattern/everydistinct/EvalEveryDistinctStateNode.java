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
package com.espertech.esper.common.internal.epl.pattern.everydistinct;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.*;
import com.espertech.esper.common.internal.epl.pattern.every.EvalEveryStateNode;
import com.espertech.esper.common.internal.epl.pattern.every.EvalEveryStateSpawnEvaluator;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Contains the state collected by an "every" operator. The state includes handles to any sub-listeners
 * started by the operator.
 */
public class EvalEveryDistinctStateNode extends EvalStateNode implements Evaluator {
    protected final EvalEveryDistinctNode everyDistinctNode;
    protected final Map<EvalStateNode, Set<Object>> spawnedNodes;
    protected MatchedEventMap beginState;

    /**
     * Constructor.
     *
     * @param parentNode        is the parent evaluator to call to indicate truth value
     * @param everyDistinctNode is the factory node associated to the state
     */
    public EvalEveryDistinctStateNode(Evaluator parentNode,
                                      EvalEveryDistinctNode everyDistinctNode) {
        super(parentNode);

        this.everyDistinctNode = everyDistinctNode;
        this.spawnedNodes = new LinkedHashMap<EvalStateNode, Set<Object>>();
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        if (PatternConsumptionUtil.containsEvent(matchEvent, beginState)) {
            quit();
            AgentInstanceContext agentInstanceContext = everyDistinctNode.getContext().getAgentInstanceContext();
            agentInstanceContext.getAuditProvider().patternFalse(everyDistinctNode.getFactoryNode(), this, agentInstanceContext);
            this.getParentEvaluator().evaluateFalse(this, true);
        } else {
            PatternConsumptionUtil.childNodeRemoveMatches(matchEvent, spawnedNodes.keySet());
        }
    }

    @Override
    public EvalNode getFactoryNode() {
        return everyDistinctNode;
    }

    public final void start(MatchedEventMap beginState) {
        AgentInstanceContext agentInstanceContext = everyDistinctNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternEveryDistinctStart(everyDistinctNode.factoryNode, beginState);
        agentInstanceContext.getAuditProvider().patternInstance(true, everyDistinctNode.factoryNode, agentInstanceContext);

        this.beginState = beginState.shallowCopy();
        EvalStateNode childState = everyDistinctNode.getChildNode().newState(this);
        spawnedNodes.put(childState, new HashSet<Object>());

        if (spawnedNodes.size() != 1) {
            throw new IllegalStateException("EVERY state node is expected to have single child state node");
        }

        // During the start of the child we need to use the temporary evaluator to catch any event created during a start.
        // Events created during the start would likely come from the "not" operator.
        // Quit the new child again if
        EvalEveryStateSpawnEvaluator spawnEvaluator = new EvalEveryStateSpawnEvaluator(everyDistinctNode.getContext().getStatementName());
        childState.setParentEvaluator(spawnEvaluator);
        childState.start(beginState);

        // If the spawned expression turned true already, just quit it
        if (spawnEvaluator.isEvaluatedTrue()) {
            childState.quit();
        } else {
            childState.setParentEvaluator(this);
        }

        agentInstanceContext.getInstrumentationProvider().aPatternEveryDistinctStart();
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        AgentInstanceContext agentInstanceContext = everyDistinctNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternEveryDistinctEvalFalse(everyDistinctNode.factoryNode);

        fromNode.quit();
        spawnedNodes.remove(fromNode);

        // Spawn all nodes below this EVERY node
        // During the start of a child we need to use the temporary evaluator to catch any event created during a start
        // Such events can be raised when the "not" operator is used.
        EvalEveryStateSpawnEvaluator spawnEvaluator = new EvalEveryStateSpawnEvaluator(everyDistinctNode.getContext().getStatementName());
        EvalStateNode spawned = everyDistinctNode.getChildNode().newState(spawnEvaluator);
        spawned.start(beginState);

        // If the whole spawned expression already turned true, quit it again
        if (spawnEvaluator.isEvaluatedTrue()) {
            spawned.quit();
        } else {
            spawnedNodes.put(spawned, new HashSet<Object>());
            spawned.setParentEvaluator(this);
        }

        agentInstanceContext.getInstrumentationProvider().aPatternEveryDistinctEvalFalse();
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        AgentInstanceContext agentInstanceContext = everyDistinctNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternEveryDistinctEvaluateTrue(everyDistinctNode.factoryNode, matchEvent);

        // determine if this evaluation has been seen before from the same node
        Object matchEventKey = PatternExpressionUtil.getKeys(matchEvent, everyDistinctNode.getFactoryNode().getConvertor(), everyDistinctNode.getFactoryNode().getDistinctExpression(), everyDistinctNode.getContext().getAgentInstanceContext());
        boolean haveSeenThis = false;
        Set<Object> keysFromNode = spawnedNodes.get(fromNode);
        if (keysFromNode != null) {
            if (keysFromNode.contains(matchEventKey)) {
                haveSeenThis = true;
            } else {
                keysFromNode.add(matchEventKey);
            }
        }

        if (isQuitted) {
            spawnedNodes.remove(fromNode);
        }

        // See explanation in EvalFilterStateNode for the type check
        if (fromNode.isFilterStateNode()) {
            // We do not need to newState new listeners here, since the filter state node below this node did not quit
        } else {
            // Spawn all nodes below this EVERY node
            // During the start of a child we need to use the temporary evaluator to catch any event created during a start
            // Such events can be raised when the "not" operator is used.
            EvalEveryStateSpawnEvaluator spawnEvaluator = new EvalEveryStateSpawnEvaluator(everyDistinctNode.getContext().getStatementName());
            EvalStateNode spawned = everyDistinctNode.getChildNode().newState(spawnEvaluator);
            spawned.start(beginState);

            // If the whole spawned expression already turned true, quit it again
            if (spawnEvaluator.isEvaluatedTrue()) {
                spawned.quit();
            } else {
                Set<Object> keyset = new HashSet<Object>();
                if (keysFromNode != null) {
                    keyset.addAll(keysFromNode);
                }
                spawnedNodes.put(spawned, keyset);
                spawned.setParentEvaluator(this);
            }
        }

        if (!haveSeenThis) {
            agentInstanceContext.getAuditProvider().patternTrue(everyDistinctNode.getFactoryNode(), this, matchEvent, false, agentInstanceContext);
            this.getParentEvaluator().evaluateTrue(matchEvent, this, false, optionalTriggeringEvent);
        }

        agentInstanceContext.getInstrumentationProvider().aPatternEveryDistinctEvaluateTrue(keysFromNode, null, matchEventKey, haveSeenThis);
    }

    public final void quit() {
        AgentInstanceContext agentInstanceContext = everyDistinctNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternEveryDistinctQuit(everyDistinctNode.factoryNode);
        agentInstanceContext.getAuditProvider().patternInstance(false, everyDistinctNode.factoryNode, agentInstanceContext);

        // Stop all child nodes
        for (EvalStateNode child : spawnedNodes.keySet()) {
            child.quit();
        }

        agentInstanceContext.getInstrumentationProvider().aPatternEveryDistinctQuit();
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitEveryDistinct(everyDistinctNode.getFactoryNode(), this, beginState, spawnedNodes.values());
        for (EvalStateNode spawnedNode : spawnedNodes.keySet()) {
            spawnedNode.accept(visitor);
        }
    }

    public boolean isFilterStateNode() {
        return false;
    }

    public boolean isNotOperator() {
        return false;
    }

    public boolean isFilterChildNonQuitting() {
        return true;
    }

    public boolean isObserverStateNodeNonRestarting() {
        return false;
    }

    public final String toString() {
        return "EvalEveryStateNode spawnedChildren=" + spawnedNodes.size();
    }

    private static final Logger log = LoggerFactory.getLogger(EvalEveryStateNode.class);
}
