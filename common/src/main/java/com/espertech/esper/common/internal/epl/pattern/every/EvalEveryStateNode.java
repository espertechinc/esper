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
package com.espertech.esper.common.internal.epl.pattern.every;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.*;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
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
            AgentInstanceContext agentInstanceContext = evalEveryNode.getContext().getAgentInstanceContext();
            agentInstanceContext.getAuditProvider().patternFalse(evalEveryNode.getFactoryNode(), this, agentInstanceContext);
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
        AgentInstanceContext agentInstanceContext = evalEveryNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternEveryStart(evalEveryNode.factoryNode, beginState);
        agentInstanceContext.getAuditProvider().patternInstance(true, evalEveryNode.factoryNode, agentInstanceContext);

        this.beginState = beginState.shallowCopy();
        EvalStateNode childState = evalEveryNode.getChildNode().newState(this);
        spawnedNodes.add(childState);

        // During the start of the child we need to use the temporary evaluator to catch any event created during a start.
        // Events created during the start would likely come from the "not" operator.
        // Quit the new child again if
        EvalEveryStateSpawnEvaluator spawnEvaluator = new EvalEveryStateSpawnEvaluator(evalEveryNode.getContext().getStatementName());
        childState.setParentEvaluator(spawnEvaluator);
        childState.start(beginState);

        // If the spawned expression turned true already, just quit it
        if (spawnEvaluator.isEvaluatedTrue()) {
            childState.quit();
        } else {
            childState.setParentEvaluator(this);
        }

        agentInstanceContext.getInstrumentationProvider().aPatternEveryStart();
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        AgentInstanceContext agentInstanceContext = evalEveryNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternEveryEvalFalse(evalEveryNode.factoryNode);

        fromNode.quit();
        spawnedNodes.remove(fromNode);

        if (!restartable) {
            agentInstanceContext.getAuditProvider().patternFalse(evalEveryNode.getFactoryNode(), this, agentInstanceContext);
            agentInstanceContext.getAuditProvider().patternInstance(false, evalEveryNode.factoryNode, agentInstanceContext);
            getParentEvaluator().evaluateFalse(this, false);
            return;
        }

        // Spawn all nodes below this EVERY node
        // During the start of a child we need to use the temporary evaluator to catch any event created during a start
        // Such events can be raised when the "not" operator is used.
        EvalEveryStateSpawnEvaluator spawnEvaluator = new EvalEveryStateSpawnEvaluator(evalEveryNode.getContext().getStatementName());
        EvalStateNode spawned = evalEveryNode.getChildNode().newState(spawnEvaluator);
        spawned.start(beginState);

        // If the whole spawned expression already turned true, quit it again
        if (spawnEvaluator.isEvaluatedTrue()) {
            spawned.quit();
        } else {
            spawnedNodes.add(spawned);
            spawned.setParentEvaluator(this);
        }

        agentInstanceContext.getInstrumentationProvider().aPatternEveryEvalFalse();
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        AgentInstanceContext agentInstanceContext = evalEveryNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternEveryEvaluateTrue(evalEveryNode.factoryNode, matchEvent);

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
            EvalEveryStateSpawnEvaluator spawnEvaluator = new EvalEveryStateSpawnEvaluator(evalEveryNode.getContext().getStatementName());
            EvalStateNode spawned = evalEveryNode.getChildNode().newState(spawnEvaluator);
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
        agentInstanceContext.getAuditProvider().patternTrue(evalEveryNode.getFactoryNode(), this, matchEvent, false, agentInstanceContext);
        this.getParentEvaluator().evaluateTrue(matchEvent, this, false, optionalTriggeringEvent);

        agentInstanceContext.getInstrumentationProvider().aPatternEveryEvaluateTrue();
    }

    public final void quit() {
        AgentInstanceContext agentInstanceContext = evalEveryNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternEveryQuit(evalEveryNode.factoryNode);
        agentInstanceContext.getAuditProvider().patternInstance(false, evalEveryNode.factoryNode, agentInstanceContext);

        // Stop all child nodes
        for (EvalStateNode child : spawnedNodes) {
            child.quit();
        }

        agentInstanceContext.getInstrumentationProvider().aPatternEveryQuit();
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
