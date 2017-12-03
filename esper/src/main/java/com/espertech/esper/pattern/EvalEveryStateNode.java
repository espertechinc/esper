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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Contains the state collected by an "every" operator. The state includes handles to any sub-listeners
 * started by the operator.
 */
public class EvalEveryStateNode extends EvalStateNode implements Evaluator {
    protected final EvalEveryNode evalEveryNode;
    protected final List<EvalStateNode> spawnedNodes;
    protected MatchedEventMap beginState;

    /**
     * Constructor.
     *
     * @param parentNode    is the parent evaluator to call to indicate truth value
     * @param evalEveryNode is the factory node associated to the state
     */
    public EvalEveryStateNode(Evaluator parentNode,
                              EvalEveryNode evalEveryNode) {
        super(parentNode);

        this.evalEveryNode = evalEveryNode;
        this.spawnedNodes = new ArrayList<EvalStateNode>();
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        if (PatternConsumptionUtil.containsEvent(matchEvent, beginState)) {
            quit();
            this.getParentEvaluator().evaluateFalse(this, true);
        } else {
            PatternConsumptionUtil.childNodeRemoveMatches(matchEvent, spawnedNodes);
        }
    }

    @Override
    public EvalNode getFactoryNode() {
        return evalEveryNode;
    }

    public final void start(MatchedEventMap beginState) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternEveryStart(evalEveryNode, beginState);
        }
        this.beginState = beginState.shallowCopy();
        EvalStateNode childState = evalEveryNode.getChildNode().newState(this, null, 0L);
        spawnedNodes.add(childState);

        // During the start of the child we need to use the temporary evaluator to catch any event created during a start.
        // Events created during the start would likely come from the "not" operator.
        // Quit the new child again if
        EvalEveryStateSpawnEvaluator spawnEvaluator = new EvalEveryStateSpawnEvaluator(evalEveryNode.getContext().getPatternContext().getStatementName());
        childState.setParentEvaluator(spawnEvaluator);
        childState.start(beginState);

        // If the spawned expression turned true already, just quit it
        if (spawnEvaluator.isEvaluatedTrue()) {
            childState.quit();
        } else {
            childState.setParentEvaluator(this);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternEveryStart();
        }
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternEveryEvalFalse(evalEveryNode);
        }
        fromNode.quit();
        spawnedNodes.remove(fromNode);

        if (!restartable) {
            getParentEvaluator().evaluateFalse(this, false);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aPatternEveryEvalFalse();
            }
            return;
        }

        // Spawn all nodes below this EVERY node
        // During the start of a child we need to use the temporary evaluator to catch any event created during a start
        // Such events can be raised when the "not" operator is used.
        EvalEveryStateSpawnEvaluator spawnEvaluator = new EvalEveryStateSpawnEvaluator(evalEveryNode.getContext().getPatternContext().getStatementName());
        EvalStateNode spawned = evalEveryNode.getChildNode().newState(spawnEvaluator, null, 0L);
        spawned.start(beginState);

        // If the whole spawned expression already turned true, quit it again
        if (spawnEvaluator.isEvaluatedTrue()) {
            spawned.quit();
        } else {
            spawnedNodes.add(spawned);
            spawned.setParentEvaluator(this);
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternEveryEvalFalse();
        }
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternEveryEvaluateTrue(evalEveryNode, matchEvent);
        }
        if (isQuitted) {
            spawnedNodes.remove(fromNode);
        }

        // See explanation in EvalFilterStateNode for the type check
        if (fromNode.isFilterStateNode() || fromNode.isObserverStateNodeNonRestarting()) {
            // We do not need to newState new listeners here, since the filter state node below this node did not quit
        } else {
            // Spawn all nodes below this EVERY node
            // During the start of a child we need to use the temporary evaluator to catch any event created during a start
            // Such events can be raised when the "not" operator is used.
            EvalEveryStateSpawnEvaluator spawnEvaluator = new EvalEveryStateSpawnEvaluator(evalEveryNode.getContext().getPatternContext().getStatementName());
            EvalStateNode spawned = evalEveryNode.getChildNode().newState(spawnEvaluator, null, 0L);
            spawned.start(beginState);

            // If the whole spawned expression already turned true, quit it again
            if (spawnEvaluator.isEvaluatedTrue()) {
                spawned.quit();
            } else {
                spawnedNodes.add(spawned);
                spawned.setParentEvaluator(this);
            }
        }

        // All nodes indicate to their parents that their child node did not quit, therefore a false for isQuitted
        this.getParentEvaluator().evaluateTrue(matchEvent, this, false, optionalTriggeringEvent);
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternEveryEvaluateTrue();
        }
    }

    public final void quit() {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qPatternEveryQuit(evalEveryNode);
        }
        // Stop all child nodes
        for (EvalStateNode child : spawnedNodes) {
            child.quit();
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aPatternEveryQuit();
        }
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitEvery(evalEveryNode.getFactoryNode(), this, beginState);
        for (EvalStateNode spawnedNode : spawnedNodes) {
            spawnedNode.accept(visitor);
        }
    }

    public boolean isNotOperator() {
        return false;
    }

    public boolean isFilterStateNode() {
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
