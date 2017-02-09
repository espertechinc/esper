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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is always the root node in the evaluation tree representing an event expression.
 * It hold the handle to the EPStatement implementation for notifying when matches are found.
 */
public class EvalRootFactoryNode extends EvalNodeFactoryBase {
    private static final long serialVersionUID = -4478876398666926782L;

    public final int numTreeChildNodes;

    public EvalRootFactoryNode(EvalFactoryNode childNode) {
        addChildNode(childNode);
        this.numTreeChildNodes = assignFactoryNodeIds();
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        EvalNode child = EvalNodeUtil.makeEvalNodeSingleChild(this.getChildNodes(), agentInstanceContext, parentNode);
        return new EvalRootNode(agentInstanceContext, this, child);
    }

    public final String toString() {
        return "EvalRootNode children=" + this.getChildNodes().size();
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public boolean isStateful() {
        return this.getChildNodes().get(0).isStateful();
    }

    public int getNumTreeChildNodes() {
        return numTreeChildNodes;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        if (!getChildNodes().isEmpty()) {
            getChildNodes().get(0).toEPL(writer, getPrecedence());
        }
    }

    public PatternExpressionPrecedenceEnum getPrecedence() {
        return PatternExpressionPrecedenceEnum.MINIMUM;
    }

    // assign factory ids, a short-type number assigned once-per-statement to each pattern node
    // return the count of all ids
    private int assignFactoryNodeIds() {
        short count = 0;
        setFactoryNodeId(count);
        List<EvalFactoryNode> factories = collectFactories(this);
        for (EvalFactoryNode factoryNode : factories) {
            count++;
            factoryNode.setFactoryNodeId(count);
        }
        return count;
    }

    private static List<EvalFactoryNode> collectFactories(EvalRootFactoryNode rootFactory) {
        List<EvalFactoryNode> factories = new ArrayList<EvalFactoryNode>(8);
        for (EvalFactoryNode factoryNode : rootFactory.getChildNodes()) {
            collectFactoriesRecursive(factoryNode, factories);
        }
        return factories;
    }

    private static void collectFactoriesRecursive(EvalFactoryNode factoryNode, List<EvalFactoryNode> factories) {
        factories.add(factoryNode);
        for (EvalFactoryNode childNode : factoryNode.getChildNodes()) {
            collectFactoriesRecursive(childNode, factories);
        }
    }
}
