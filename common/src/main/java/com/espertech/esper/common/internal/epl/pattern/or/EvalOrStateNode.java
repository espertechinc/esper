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
package com.espertech.esper.common.internal.epl.pattern.or;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.pattern.core.EvalNode;
import com.espertech.esper.common.internal.epl.pattern.core.EvalStateNode;
import com.espertech.esper.common.internal.epl.pattern.core.EvalStateNodeVisitor;
import com.espertech.esper.common.internal.epl.pattern.core.Evaluator;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;

/**
 * This class represents the state of a "or" operator in the evaluation state tree.
 */
public class EvalOrStateNode extends EvalStateNode implements Evaluator {
    private final EvalOrNode evalOrNode;
    private final EvalStateNode[] childNodes;
    private boolean quitted;

    /**
     * Constructor.
     *
     * @param parentNode is the parent evaluator to call to indicate truth value
     * @param evalOrNode is the factory node associated to the state
     */
    public EvalOrStateNode(Evaluator parentNode,
                           EvalOrNode evalOrNode) {
        super(parentNode);

        this.childNodes = new EvalStateNode[evalOrNode.getChildNodes().length];
        this.evalOrNode = evalOrNode;
    }

    public void removeMatch(Set<EventBean> matchEvent) {
        for (EvalStateNode node : childNodes) {
            if (node != null) {
                node.removeMatch(matchEvent);
            }
        }
    }

    @Override
    public EvalNode getFactoryNode() {
        return evalOrNode;
    }

    public final void start(MatchedEventMap beginState) {
        AgentInstanceContext agentInstanceContext = evalOrNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternOrStart(evalOrNode.factoryNode, beginState);
        agentInstanceContext.getAuditProvider().patternInstance(true, evalOrNode.factoryNode, agentInstanceContext);

        // In an "or" expression we need to create states for all child expressions/listeners,
        // since all are going to be started
        int count = 0;
        for (EvalNode node : evalOrNode.getChildNodes()) {
            EvalStateNode childState = node.newState(this);
            childNodes[count++] = childState;
        }

        // In an "or" expression we start all child listeners
        EvalStateNode[] childNodeCopy = new EvalStateNode[childNodes.length];
        System.arraycopy(childNodes, 0, childNodeCopy, 0, childNodes.length);
        for (EvalStateNode child : childNodeCopy) {
            child.start(beginState);
            if (quitted) {
                break;
            }
        }

        agentInstanceContext.getInstrumentationProvider().aPatternOrStart();
    }

    public final void evaluateTrue(MatchedEventMap matchEvent, EvalStateNode fromNode, boolean isQuitted, EventBean optionalTriggeringEvent) {
        AgentInstanceContext agentInstanceContext = evalOrNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternOrEvaluateTrue(evalOrNode.factoryNode, matchEvent);

        // If one of the children quits, the whole or expression turns true and all subexpressions must quit
        if (isQuitted) {
            for (int i = 0; i < childNodes.length; i++) {
                if (childNodes[i] == fromNode) {
                    childNodes[i] = null;
                }
            }
            agentInstanceContext.getAuditProvider().patternInstance(false, evalOrNode.factoryNode, agentInstanceContext);
            quitInternal();     // Quit the remaining listeners
        }

        agentInstanceContext.getAuditProvider().patternTrue(evalOrNode.getFactoryNode(), this, matchEvent, isQuitted, agentInstanceContext);
        this.getParentEvaluator().evaluateTrue(matchEvent, this, isQuitted, optionalTriggeringEvent);

        agentInstanceContext.getInstrumentationProvider().aPatternOrEvaluateTrue(isQuitted);
    }

    public final void evaluateFalse(EvalStateNode fromNode, boolean restartable) {
        AgentInstanceContext agentInstanceContext = evalOrNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternOrEvalFalse(evalOrNode.factoryNode);

        for (int i = 0; i < childNodes.length; i++) {
            if (childNodes[i] == fromNode) {
                childNodes[i] = null;
            }
        }

        boolean allEmpty = true;
        for (int i = 0; i < childNodes.length; i++) {
            if (childNodes[i] != null) {
                allEmpty = false;
                break;
            }
        }

        if (allEmpty) {
            agentInstanceContext.getAuditProvider().patternFalse(evalOrNode.getFactoryNode(), this, agentInstanceContext);
            agentInstanceContext.getAuditProvider().patternInstance(false, evalOrNode.factoryNode, agentInstanceContext);
            this.getParentEvaluator().evaluateFalse(this, true);
        }

        agentInstanceContext.getInstrumentationProvider().aPatternOrEvalFalse();
    }

    public final void quit() {
        AgentInstanceContext agentInstanceContext = evalOrNode.getContext().getAgentInstanceContext();
        agentInstanceContext.getInstrumentationProvider().qPatternOrQuit(evalOrNode.factoryNode);
        agentInstanceContext.getAuditProvider().patternInstance(false, evalOrNode.factoryNode, agentInstanceContext);

        quitInternal();

        agentInstanceContext.getInstrumentationProvider().aPatternOrQuit();
    }

    public final void accept(EvalStateNodeVisitor visitor) {
        visitor.visitOr(evalOrNode.getFactoryNode(), this);
        for (EvalStateNode node : childNodes) {
            if (node != null) {
                node.accept(visitor);
            }
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
        return "EvalOrStateNode";
    }

    private void quitInternal() {
        for (EvalStateNode child : childNodes) {
            if (child != null) {
                child.quit();
            }
        }
        Arrays.fill(childNodes, null);
        quitted = true;
    }

    private static final Logger log = LoggerFactory.getLogger(EvalOrStateNode.class);
}
