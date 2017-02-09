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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.List;

/**
 * Interface representing an expression for use in select-clauses, where-clauses, having-clauses, order-by clauses and
 * streams based on filters and pattern filter expressions.
 * <p>
 * Expressions are organized into a tree-like structure with nodes representing sub-expressions.
 * <p>
 * Certain types of nodes have certain requirements towards the number or types of nodes that
 * are expected as sub-expressions to an expression.
 */
public interface Expression extends Serializable {
    /**
     * Returns the list of sub-expressions (child expressions) to the current expression node.
     *
     * @return child expressions or empty list if there are no child expressions
     */
    public List<Expression> getChildren();

    /**
     * Sets the list of sub-expressions (child expressions) to the current expression node.
     *
     * @param children child expressions or empty list if there are no child expressions
     */
    public void setChildren(List<Expression> children);

    /**
     * Returns the tree of object name, for use by tools to assign an identifier to an expression.
     *
     * @return tree object id
     */
    public String getTreeObjectName();

    /**
     * Sets the tree of object name, for use by tools to assign an identifier to an expression.
     *
     * @param objectName tree object id
     */
    public void setTreeObjectName(String objectName);

    /**
     * Returns precedence.
     *
     * @return precedence
     */
    public ExpressionPrecedenceEnum getPrecedence();

    /**
     * Write expression considering precedence.
     *
     * @param writer           to use
     * @param parentPrecedence precedence
     */
    public void toEPL(StringWriter writer, ExpressionPrecedenceEnum parentPrecedence);
}
