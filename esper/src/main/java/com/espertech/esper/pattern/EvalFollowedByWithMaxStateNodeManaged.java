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
import com.espertech.esper.client.hook.ConditionPatternSubexpressionMax;
import com.espertech.esper.filterspec.MatchedEventMap;
import com.espertech.esper.pattern.pool.PatternSubexpressionPoolStmtSvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class represents the state of a followed-by operator in the evaluation state tree, with a maximum number of instances provided, and
 * with the additional capability to engine-wide report on pattern instances.
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
        EvalNode child = evalFollowedByNode.getChildNodes()[0];
        EvalStateNode childState = child.newState(this, null, 0L);
        nodes.put(childState, 0);
        childState.start(beginState);
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        Integer index = nodes.get(fromNode);

        if (isQuitted) {
            nodes.remove(fromNode);
            if (index != null && index > 0) {
                if (evalFollowedByNode.isTrackWithMax()) {
                    countActivePerChild[index - 1]--;
                }
                if (evalFollowedByNode.isTrackWithPool()) {
                    PatternSubexpressionPoolStmtSvc poolSvc = evalFollowedByNode.getContext().getStatementContext().getPatternSubexpressionPoolSvc();
                    poolSvc.getEngineSvc().decreaseCount(evalFollowedByNode, evalFollowedByNode.getContext().getAgentInstanceContext());
                    poolSvc.getStmtHandler().decreaseCount();
                }
            }
        }

        // the node may already have quit as a result of an outer state quitting this state,
        // however the callback may still be received; It is fine to ignore this callback. 
        if (index == null) {
            return;
        }

        // If the match came from the very last filter, need to escalate
        int numChildNodes = evalFollowedByNode.getChildNodes().length;
        if (index == (numChildNodes - 1)) {
            boolean isFollowedByQuitted = false;
            if (nodes.isEmpty()) {
                isFollowedByQuitted = true;
            }

            this.getParentEvaluator().evaluateTrue(matchEvent, this, isFollowedByQuitted, optionalTriggeringEvent);
        } else {
            // Else start a new sub-expression for the next-in-line filter
            if (evalFollowedByNode.isTrackWithMax()) {
                int max = evalFollowedByNode.getFactoryNode().getMax(index);
                if ((max != -1) && (max >= 0)) {
                    if (countActivePerChild[index] >= max) {
                        evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext().getExceptionHandlingService().handleCondition(new ConditionPatternSubexpressionMax(max), evalFollowedByNode.getContext().getAgentInstanceContext().getStatementContext().getEpStatementHandle());
                        return;
                    }
                }
            }

            if (evalFollowedByNode.isTrackWithPool()) {
                PatternSubexpressionPoolStmtSvc poolSvc = evalFollowedByNode.getContext().getStatementContext().getPatternSubexpressionPoolSvc();
                boolean allow = poolSvc.getEngineSvc().tryIncreaseCount(evalFollowedByNode, evalFollowedByNode.getContext().getAgentInstanceContext());
                if (!allow) {
                    return;
                }
                poolSvc.getStmtHandler().increaseCount();
            }

            if (evalFollowedByNode.isTrackWithMax()) {
                countActivePerChild[index]++;
            }

            EvalNode child = evalFollowedByNode.getChildNodes()[index + 1];
            EvalStateNode childState = child.newState(this, null, 0L);
            nodes.put(childState, index + 1);
            childState.start(matchEvent);
        }
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        fromNode.quit();
        Integer index = nodes.remove(fromNode);
        if (index != null && index > 0) {
            if (evalFollowedByNode.isTrackWithMax()) {
                countActivePerChild[index - 1]--;
            }
            if (evalFollowedByNode.isTrackWithPool()) {
                PatternSubexpressionPoolStmtSvc poolSvc = evalFollowedByNode.getContext().getStatementContext().getPatternSubexpressionPoolSvc();
                poolSvc.getEngineSvc().decreaseCount(evalFollowedByNode, evalFollowedByNode.getContext().getAgentInstanceContext());
                poolSvc.getStmtHandler().decreaseCount();
            }
        }

        if (nodes.isEmpty()) {
            this.getParentEvaluator().evaluateFalse(this, true);
            quit();
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

    public final void quit() {
        for (Map.Entry<EvalStateNode, Integer> entry : nodes.entrySet()) {
            entry.getKey().quit();
            if (evalFollowedByNode.isTrackWithPool()) {
                if (entry.getValue() > 0) {
                    PatternSubexpressionPoolStmtSvc poolSvc = evalFollowedByNode.getContext().getStatementContext().getPatternSubexpressionPoolSvc();
                    poolSvc.getEngineSvc().decreaseCount(evalFollowedByNode, evalFollowedByNode.getContext().getAgentInstanceContext());
                    poolSvc.getStmtHandler().decreaseCount();
                }
            }
        }
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
