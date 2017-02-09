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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Superclass of all nodes in an evaluation tree representing an event pattern expression.
 * Follows the Composite pattern. Child nodes do not carry references to parent nodes, the tree
 * is unidirectional.
 */
public abstract class EvalNodeFactoryBase implements EvalFactoryNode, Serializable {
    private static final Logger log = LoggerFactory.getLogger(EvalNodeFactoryBase.class);
    private static final long serialVersionUID = 0L;

    private final List<EvalFactoryNode> childNodes;
    private short factoryNodeId;

    public abstract EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode);

    public abstract void toPrecedenceFreeEPL(StringWriter writer);

    /**
     * Constructor creates a list of child nodes.
     */
    EvalNodeFactoryBase() {
        childNodes = new ArrayList<EvalFactoryNode>();
    }

    /**
     * Adds a child node.
     *
     * @param childNode is the child evaluation tree node to add
     */
    public void addChildNode(EvalFactoryNode childNode) {
        childNodes.add(childNode);
    }

    public void addChildNodes(Collection<EvalFactoryNode> childNodesToAdd) {
        childNodes.addAll(childNodesToAdd);
    }

    /**
     * Returns list of child nodes.
     *
     * @return list of child nodes
     */
    public List<EvalFactoryNode> getChildNodes() {
        return childNodes;
    }

    public short getFactoryNodeId() {
        return factoryNodeId;
    }

    public void setFactoryNodeId(short factoryNodeId) {
        this.factoryNodeId = factoryNodeId;
    }

    public final void toEPL(StringWriter writer, PatternExpressionPrecedenceEnum parentPrecedence) {
        if (this.getPrecedence().getLevel() < parentPrecedence.getLevel()) {
            writer.write("(");
            toPrecedenceFreeEPL(writer);
            writer.write(")");
        } else {
            toPrecedenceFreeEPL(writer);
        }
    }
}
