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
package com.espertech.esper.common.internal.epl.rowrecog.expr;

import com.espertech.esper.common.internal.compile.stage1.specmapper.ExpressionCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Base node for
 */
public abstract class RowRecogExprNode {
    private static final Logger log = LoggerFactory.getLogger(RowRecogExprNode.class);

    private List<RowRecogExprNode> childNodes;

    public abstract RowRecogExprNodePrecedenceEnum getPrecedence();
    public abstract void toPrecedenceFreeEPL(StringWriter writer);
    public abstract RowRecogExprNode checkedCopySelf(ExpressionCopier expressionCopier);

    /**
     * Constructor creates a list of child nodes.
     */
    public RowRecogExprNode() {
        childNodes = new ArrayList<RowRecogExprNode>();
    }

    public final void toEPL(StringWriter writer, RowRecogExprNodePrecedenceEnum parentPrecedence) {
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
    public final void addChildNode(RowRecogExprNode childNode) {
        childNodes.add(childNode);
    }

    /**
     * Returns list of child nodes.
     *
     * @return list of child nodes
     */
    public final List<RowRecogExprNode> getChildNodes() {
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
        for (RowRecogExprNode node : childNodes) {
            node.dumpDebug(prefix + "  ");
        }
    }

    public void accept(RowRecogExprNodeVisitor visitor) {
        acceptChildnodes(visitor, null, 0);
    }

    public void acceptChildnodes(RowRecogExprNodeVisitor visitor, RowRecogExprNode parent, int level) {
        visitor.visit(this, parent, level);
        for (RowRecogExprNode childNode : childNodes) {
            childNode.acceptChildnodes(visitor, this, level + 1);
        }
    }

    public void replaceChildNode(RowRecogExprNode nodeToReplace, List<RowRecogExprNode> replacementNodes) {
        List<RowRecogExprNode> newChildNodes = new ArrayList<RowRecogExprNode>(childNodes.size() - 1 + replacementNodes.size());
        for (RowRecogExprNode node : childNodes) {
            if (node != nodeToReplace) {
                newChildNodes.add(node);
            } else {
                newChildNodes.addAll(replacementNodes);
            }
        }
        childNodes = newChildNodes;
    }

    public RowRecogExprNode checkedCopy(ExpressionCopier expressionCopier) {
        RowRecogExprNode copy = checkedCopySelf(expressionCopier);
        for (RowRecogExprNode child : childNodes) {
            RowRecogExprNode childCopy = child.checkedCopy(expressionCopier);
            copy.addChildNode(childCopy);
        }
        return copy;
    }
}
