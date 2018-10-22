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
import com.espertech.esper.common.client.hook.condition.ConditionPatternSubexpressionMax;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.*;
import com.espertech.esper.common.internal.epl.pattern.pool.PatternSubexpressionPoolStmtSvc;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class represents the state of a followed-by operator in the evaluation state tree, with a maximum number of instances provided, and
 * with the additional capability to runtime-wide report on pattern instances.
 */
public class EvalFollowedByWithMaxStateNodeManaged extends EvalStateNode implements Evaluator {
    protected final EvalFollowedByNode evalFollowedByNode;
    protected final HashMap<EvalStateNode, Integer> nodes;
    protected final int[] countActivePerChild;

    /**
     * Constructor.
     *
     * @param parentNode         is the parent evaluator to call to indicate truth value
     * @param evalFollowedByNode is the factory node associated to the state
     */
    public EvalFollowedByWithMaxStateNodeManaged(Evaluator parentNode,
                                                 EvalFollowedByNode evalFollowedByNode) {
        super(parentNode);

        this.evalFollowedByNode = evalFollowedByNode;
        this.nodes = new HashMap<EvalStateNode, Integer>();
        if (evalFollowedByNode.isTrackWithMax()) {
            this.countActivePerChild = new int[evalFollowedByNode.getChildNodes().length - 1];
        } else {
            this.countActivePerChild = null;
        }
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
        Integer index = nodes.get(fromNode);

        AgentInstanceContext agentInstanceContext = evalFollowedByNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternFollowedByEvaluateTrue(evalFollowedByNode.factoryNode, matchEvent, index);

        if (isQuitted) {
            nodes.remove(fromNode);
            if (index != null && index > 0) {
                if (evalFollowedByNode.isTrackWithMax()) {
                    countActivePerChild[index - 1]--;
                }
                if (evalFollowedByNode.isTrackWithPool()) {
                    PatternSubexpressionPoolStmtSvc poolSvc = evalFollowedByNode.getContext().getStatementContext().getPatternSubexpressionPoolSvc();
                    poolSvc.getRuntimeSvc().decreaseCount(evalFollowedByNode, evalFollowedByNode.getContext().getAgentInstanceContext());
                    poolSvc.getStmtHandler().decreaseCount();
                }
            }
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
            if (evalFollowedByNode.isTrackWithMax()) {
                int max = evalFollowedByNode.getFactoryNode().getMax(index);
                if ((max != -1) && (max >= 0)) {
                    if (countActivePerChild[index] >= max) {
                        evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext().getExceptionHandlingService().handleCondition(new ConditionPatternSubexpressionMax(max), evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext());
                        return;
                    }
                }
            }

            if (evalFollowedByNode.isTrackWithPool()) {
                PatternSubexpressionPoolStmtSvc poolSvc = evalFollowedByNode.getContext().getStatementContext().getPatternSubexpressionPoolSvc();
                boolean allow = poolSvc.getRuntimeSvc().tryIncreaseCount(evalFollowedByNode, evalFollowedByNode.getContext().getAgentInstanceContext());
                if (!allow) {
                    return;
                }
                poolSvc.getStmtHandler().increaseCount();
            }

            if (evalFollowedByNode.isTrackWithMax()) {
                countActivePerChild[index]++;
            }

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
        Integer index = nodes.remove(fromNode);
        if (index != null && index > 0) {
            if (evalFollowedByNode.isTrackWithMax()) {
                countActivePerChild[index - 1]--;
            }
            if (evalFollowedByNode.isTrackWithPool()) {
                PatternSubexpressionPoolStmtSvc poolSvc = evalFollowedByNode.getContext().getStatementContext().getPatternSubexpressionPoolSvc();
                poolSvc.getRuntimeSvc().decreaseCount(evalFollowedByNode, evalFollowedByNode.getContext().getAgentInstanceContext());
                poolSvc.getStmtHandler().decreaseCount();
            }
        }

        if (nodes.isEmpty()) {
            agentInstanceContext.getAuditProvider().patternFalse(evalFollowedByNode.getFactoryNode(), this, agentInstanceContext);
            this.getParentEvaluator().evaluateFalse(this, true);
            quit();
        }

        agentInstanceContext.getInstrumentationProvider().aPatternFollowedByEvalFalse();
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

    public final void quit() {
        AgentInstanceContext agentInstanceContext = evalFollowedByNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternFollowedByQuit(evalFollowedByNode.factoryNode);
        agentInstanceContext.getAuditProvider().patternInstance(false, evalFollowedByNode.factoryNode, agentInstanceContext);

        for (Map.Entry<EvalStateNode, Integer> entry : nodes.entrySet()) {
            entry.getKey().quit();
            if (evalFollowedByNode.isTrackWithPool()) {
                if (entry.getValue() > 0) {
                    PatternSubexpressionPoolStmtSvc poolSvc = evalFollowedByNode.getContext().getStatementContext().getPatternSubexpressionPoolSvc();
                    poolSvc.getRuntimeSvc().decreaseCount(evalFollowedByNode, evalFollowedByNode.getContext().getAgentInstanceContext());
                    poolSvc.getStmtHandler().decreaseCount();
                }
            }
        }

        agentInstanceContext.getInstrumentationProvider().aPatternFollowedByQuit();
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitFollowedBy(evalFollowedByNode.getFactoryNode(), this, nodes, countActivePerChild);
        for (EvalStateNode node : nodes.keySet()) {
            node.accept(visitor);
        }
    }

    public final String toString() {
        return "EvalFollowedByStateNode nodes=" + nodes.size();
    }
}
