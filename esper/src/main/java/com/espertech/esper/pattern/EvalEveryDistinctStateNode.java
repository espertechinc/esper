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
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
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
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternEveryDistinctStart(everyDistinctNode, beginState);
        }
        this.beginState = beginState.shallowCopy();
        EvalStateNode childState = everyDistinctNode.getChildNode().newState(this, null, 0L);
        spawnedNodes.put(childState, new HashSet<Object>());

        if (spawnedNodes.size() != 1) {
            throw new IllegalStateException("EVERY state node is expected to have single child state node");
        }

        // During the start of the child we need to use the temporary evaluator to catch any event created during a start.
        // Events created during the start would likely come from the "not" operator.
        // Quit the new child again if
        EvalEveryStateSpawnEvaluator spawnEvaluator = new EvalEveryStateSpawnEvaluator(everyDistinctNode.getContext().getPatternContext().getStatementName());
        childState.setParentEvaluator(spawnEvaluator);
        childState.start(beginState);

        // If the spawned expression turned true already, just quit it
        if (spawnEvaluator.isEvaluatedTrue()) {
            childState.quit();
        } else {
            childState.setParentEvaluator(this);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternEveryDistinctStart();
        }
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternEveryDistinctEvalFalse(everyDistinctNode);
        }
        fromNode.quit();
        spawnedNodes.remove(fromNode);

        // Spawn all nodes below this EVERY node
        // During the start of a child we need to use the temporary evaluator to catch any event created during a start
        // Such events can be raised when the "not" operator is used.
        EvalEveryStateSpawnEvaluator spawnEvaluator = new EvalEveryStateSpawnEvaluator(everyDistinctNode.getContext().getPatternContext().getStatementName());
        EvalStateNode spawned = everyDistinctNode.getChildNode().newState(spawnEvaluator, null, 0L);
        spawned.start(beginState);

        // If the whole spawned expression already turned true, quit it again
        if (spawnEvaluator.isEvaluatedTrue()) {
            spawned.quit();
        } else {
            spawnedNodes.put(spawned, new HashSet<Object>());
            spawned.setParentEvaluator(this);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternEveryDistinctEvalFalse();
        }
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternEveryDistinctEvaluateTrue(everyDistinctNode, matchEvent);
        }

        // determine if this evaluation has been seen before from the same node
        Object matchEventKey = PatternExpressionUtil.getKeys(matchEvent, everyDistinctNode.getFactoryNode().getConvertor(), everyDistinctNode.getFactoryNode().getDistinctExpressionsArray(), everyDistinctNode.getContext().getAgentInstanceContext());
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
            EvalEveryStateSpawnEvaluator spawnEvaluator = new EvalEveryStateSpawnEvaluator(everyDistinctNode.getContext().getPatternContext().getStatementName());
            EvalStateNode spawned = everyDistinctNode.getChildNode().newState(spawnEvaluator, null, 0L);
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
            this.getParentEvaluator().evaluateTrue(matchEvent, this, false, optionalTriggeringEvent);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternEveryDistinctEvaluateTrue(keysFromNode, null, matchEventKey, haveSeenThis);
        }
    }

    public final void quit() {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternEveryDistinctQuit(everyDistinctNode);
        }
        // Stop all child nodes
        for (EvalStateNode child : spawnedNodes.keySet()) {
            child.quit();
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternEveryDistinctQuit();
        }
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
