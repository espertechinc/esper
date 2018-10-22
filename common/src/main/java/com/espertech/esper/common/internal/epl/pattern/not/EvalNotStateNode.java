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
package com.espertech.esper.common.internal.epl.pattern.not;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.EvalNode;
import com.espertech.esper.common.internal.epl.pattern.core.EvalStateNode;
import com.espertech.esper.common.internal.epl.pattern.core.EvalStateNodeVisitor;
import com.espertech.esper.common.internal.epl.pattern.core.Evaluator;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * This class contains the state of an 'not' operator in the evaluation state tree.
 * The not operator inverts the truth of the subexpression under it. It defaults to being true rather than
 * being false at startup. True at startup means it will generate an event on newState such that parent expressions
 * may turn true. It turns permenantly false when it receives an event from a subexpression and the subexpression
 * quitted. It indicates the false state via an evaluateFalse call on its parent evaluator.
 */
public class EvalNotStateNode extends EvalStateNode implements Evaluator {
    protected final EvalNotNode evalNotNode;
    protected EvalStateNode childNode;

    /**
     * Constructor.
     *
     * @param parentNode  is the parent evaluator to call to indicate truth value
     * @param evalNotNode is the factory node associated to the state
     */
    public EvalNotStateNode(Evaluator parentNode,
                            EvalNotNode evalNotNode) {
        super(parentNode);

        this.evalNotNode = evalNotNode;
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        // The not-operator does not pass along the matches
    }

    @Override
    public EvalNode getFactoryNode() {
        return evalNotNode;
    }

    public final void start(MatchedEventMap beginState) {
        EvalNotFactoryNode factoryNode = evalNotNode.getFactoryNode();
        AgentInstanceContext agentInstanceContext = evalNotNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternNotStart(evalNotNode.factoryNode, beginState);
        agentInstanceContext.getAuditProvider().patternInstance(true, factoryNode, agentInstanceContext);

        childNode = evalNotNode.getChildNode().newState(this);
        childNode.start(beginState);

        // The not node acts by inverting the truth
        // By default the child nodes are false. This not node acts inverts the truth and pretends the child is true,
        // raising an event up.
        agentInstanceContext.getAuditProvider().patternTrue(factoryNode, this, beginState, false, agentInstanceContext);
        this.getParentEvaluator().evaluateTrue(beginState, this, false, null);
        agentInstanceContext.getInstrumentationProvider().aPatternNotStart();
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        AgentInstanceContext agentInstanceContext = evalNotNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternNotEvalFalse(evalNotNode.factoryNode);
        agentInstanceContext.getInstrumentationProvider().aPatternNotEvalFalse();
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        AgentInstanceContext agentInstanceContext = evalNotNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternNotEvaluateTrue(evalNotNode.factoryNode, matchEvent);

        // Only is the subexpression stopped listening can we tell the parent evaluator that this
        // turned permanently false.
        if (isQuitted) {
            childNode = null;
            agentInstanceContext.getAuditProvider().patternFalse(evalNotNode.getFactoryNode(), this, agentInstanceContext);
            agentInstanceContext.getAuditProvider().patternInstance(false, evalNotNode.factoryNode, agentInstanceContext);
            this.getParentEvaluator().evaluateFalse(this, true);
        } else {
            // If the subexpression did not quit, we stay in the "true" state
        }

        agentInstanceContext.getInstrumentationProvider().aPatternNotEvaluateTrue(isQuitted);
    }

    public final void quit() {
        AgentInstanceContext agentInstanceContext = evalNotNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternNotQuit(evalNotNode.factoryNode);
        agentInstanceContext.getAuditProvider().patternInstance(false, evalNotNode.factoryNode, agentInstanceContext);

        if (childNode != null) {
            childNode.quit();
        }

        agentInstanceContext.getInstrumentationProvider().aPatternNotQuit();
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitNot(evalNotNode.getFactoryNode(), this);
        if (childNode != null) {
            childNode.accept(visitor);
        }
    }

    public boolean isNotOperator() {
        return true;
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
        return "EvalNotStateNode child=" + childNode;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalNotStateNode.class);
}
