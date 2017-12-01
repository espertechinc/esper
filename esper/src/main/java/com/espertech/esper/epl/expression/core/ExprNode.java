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

import java.io.Serializable;
import java.util.Collection;

public interface ExprNode extends ExprNodeRenderable, ExprValidator, Serializable {

    /**
     * Returns precedence.
     *
     * @return precedence
     */
    public ExprPrecedenceEnum getPrecedence();

    /**
     * Returns true if the expression node's evaluation value doesn't depend on any events data,
     * as must be determined at validation time, which is bottom-up and therefore
     * reliably allows each node to determine constant value.
     *
     * @return true for constant evaluation value, false for non-constant evaluation value
     */
    public boolean isConstantResult();

    /**
     * Return true if a expression node semantically equals the current node, or false if not.
     * <p>Concrete implementations should compare the type and any additional information
     * that impact the evaluation of a node.
     *
     * @param node               to compare to
     * @param ignoreStreamPrefix when the equals-comparison can ignore prefix of event properties
     * @return true if semantically equal, or false if not equals
     */
    public boolean equalsNode(ExprNode node, boolean ignoreStreamPrefix);

    /**
     * Accept the visitor. The visitor will first visit the parent then visit all child nodes, then their child nodes.
     * <p>The visitor can decide to skip child nodes by returning false in isVisit.
     *
     * @param visitor to visit each node and each child node.
     */
    public void accept(ExprNodeVisitor visitor);

    /**
     * Accept the visitor. The visitor will first visit the parent then visit all child nodes, then their child nodes.
     * <p>The visitor can decide to skip child nodes by returning false in isVisit.
     *
     * @param visitor to visit each node and each child node.
     */
    public void accept(ExprNodeVisitorWithParent visitor);

    /**
     * Accept a visitor that receives both parent and child node.
     *
     * @param visitor to apply
     * @param parent  node
     */
    public void acceptChildnodes(ExprNodeVisitorWithParent visitor, ExprNode parent);

    /**
     * Adds a child node.
     *
     * @param childNode is the child evaluation tree node to add
     */
    public void addChildNode(ExprNode childNode);

    /**
     * Adds child nodes.
     *
     * @param childNodes are the child evaluation tree node to add
     */
    public void addChildNodes(Collection<ExprNode> childNodes);

    /**
     * Returns list of child nodes.
     *
     * @return list of child nodes
     */
    public ExprNode[] getChildNodes();

    public void replaceUnlistedChildNode(ExprNode nodeToReplace, ExprNode newNode);

    public void setChildNode(int index, ExprNode newNode);

    public void setChildNodes(ExprNode... nodes);

    ExprForge getForge();
}
