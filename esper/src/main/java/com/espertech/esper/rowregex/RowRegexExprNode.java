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
package com.espertech.esper.rowregex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Base node for
 */
public abstract class RowRegexExprNode implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(RowRegexExprNode.class);
    private static final long serialVersionUID = 0L;

    private List<RowRegexExprNode> childNodes;

    public abstract RowRegexExprNodePrecedenceEnum getPrecedence();

    public abstract void toPrecedenceFreeEPL(StringWriter writer);

    /**
     * Constructor creates a list of child nodes.
     */
    public RowRegexExprNode() {
        childNodes = new ArrayList<RowRegexExprNode>();
    }

    public final void toEPL(StringWriter writer, RowRegexExprNodePrecedenceEnum parentPrecedence) {
        if (this.getPrecedence().getLevel() < parentPrecedence.getLevel()) {
            writer.write("(");
            toPrecedenceFreeEPL(writer);
            writer.write(")");
        } else {
            toPrecedenceFreeEPL(writer);
        }
    }

    /**
     * Adds a child node.
     *
     * @param childNode is the child evaluation tree node to add
     */
    public final void addChildNode(RowRegexExprNode childNode) {
        childNodes.add(childNode);
    }

    /**
     * Returns list of child nodes.
     *
     * @return list of child nodes
     */
    public final List<RowRegexExprNode> getChildNodes() {
        return childNodes;
    }

    /**
     * Recursively print out all nodes.
     *
     * @param prefix is printed out for naming the printed info
     */
    @SuppressWarnings({"StringContatenationInLoop"})
    public final void dumpDebug(String prefix) {
        if (log.isDebugEnabled()) {
            log.debug(".dumpDebug " + prefix + this.toString());
        }
        for (RowRegexExprNode node : childNodes) {
            node.dumpDebug(prefix + "  ");
        }
    }

    public void accept(RowRegexExprNodeVisitor visitor) {
        acceptChildnodes(visitor, null, 0);
    }

    public void acceptChildnodes(RowRegexExprNodeVisitor visitor, RowRegexExprNode parent, int level) {
        visitor.visit(this, parent, level);
        for (RowRegexExprNode childNode : childNodes) {
            childNode.acceptChildnodes(visitor, this, level + 1);
        }
    }

    public void replaceChildNode(RowRegexExprNode nodeToReplace, List<RowRegexExprNode> replacementNodes) {
        List<RowRegexExprNode> newChildNodes = new ArrayList<RowRegexExprNode>(childNodes.size() - 1 + replacementNodes.size());
        for (RowRegexExprNode node : childNodes) {
            if (node != nodeToReplace) {
                newChildNodes.add(node);
            } else {
                newChildNodes.addAll(replacementNodes);
            }
        }
        childNodes = newChildNodes;
    }
}
