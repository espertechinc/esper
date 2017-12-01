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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.epl.expression.visitor.ExprNodeVisitor;
import com.espertech.esper.epl.expression.visitor.ExprNodeVisitorWithParent;
import com.espertech.esper.util.CollectionUtil;

import java.io.StringWriter;
import java.util.Collection;

/**
 * Superclass for filter nodes in a filter expression tree. Allow
 * validation against stream event types and evaluation of events against filter tree.
 */
public abstract class ExprNodeBase implements ExprNode {
    private static final long serialVersionUID = 0L;

    private ExprNode[] childNodes;

    /**
     * Constructor creates a list of child nodes.
     */
    public ExprNodeBase() {
        childNodes = ExprNodeUtilityCore.EMPTY_EXPR_ARRAY;
    }

    public abstract void toPrecedenceFreeEPL(StringWriter writer);

    public void accept(ExprNodeVisitor visitor) {
        if (visitor.isVisit(this)) {
            visitor.visit(this);

            for (ExprNode childNode : childNodes) {
                childNode.accept(visitor);
            }
        }
    }

    public void accept(ExprNodeVisitorWithParent visitor) {
        if (visitor.isVisit(this)) {
            visitor.visit(this, null);

            for (ExprNode childNode : childNodes) {
                childNode.acceptChildnodes(visitor, this);
            }
        }
    }

    public void acceptChildnodes(ExprNodeVisitorWithParent visitor, ExprNode parent) {
        if (visitor.isVisit(this)) {
            visitor.visit(this, parent);

            for (ExprNode childNode : childNodes) {
                childNode.acceptChildnodes(visitor, this);
            }
        }
    }

    public final void addChildNode(ExprNode childNode) {
        childNodes = (ExprNode[]) CollectionUtil.arrayExpandAddSingle(childNodes, childNode);
    }

    public final void addChildNodes(Collection<ExprNode> childNodeColl) {
        childNodes = (ExprNode[]) CollectionUtil.arrayExpandAddElements(childNodes, childNodeColl);
    }

    public final ExprNode[] getChildNodes() {
        return childNodes;
    }

    public void replaceUnlistedChildNode(ExprNode nodeToReplace, ExprNode newNode) {
        // Override to replace child expression nodes that are chained or otherwise not listed as child nodes
    }

    public void addChildNodeToFront(ExprNode childNode) {
        childNodes = (ExprNode[]) CollectionUtil.arrayExpandAddElements(new ExprNode[]{childNode}, childNodes);
    }

    public void setChildNodes(ExprNode... nodes) {
        this.childNodes = nodes;
    }

    public void setChildNode(int index, ExprNode newNode) {
        this.childNodes[index] = newNode;
    }

    public void toEPL(StringWriter writer, ExprPrecedenceEnum parentPrecedence) {
        if (this.getPrecedence().getLevel() < parentPrecedence.getLevel()) {
            writer.write("(");
            toPrecedenceFreeEPL(writer);
            writer.write(")");
        } else {
            toPrecedenceFreeEPL(writer);
        }
    }

    protected static void checkValidated(ExprForge forge) {
        if (forge == null) {
            throw checkValidatedException();
        }
    }

    protected static IllegalStateException checkValidatedException() {
        return new IllegalStateException("Expression has not been validated");
    }
}

