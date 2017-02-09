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
 * Interface representing a pattern expression.
 * <p>
 * Pattern expressions are organized into a tree-like structure with nodes representing sub-expressions (composite).
 * <p>
 * Certain types of nodes have certain requirements towards the number or types of nodes that
 * are expected as pattern sub-expressions to an pattern expression.
 */
public interface PatternExpr extends Serializable {
    /**
     * Returns the list of pattern sub-expressions (child expressions) to the current pattern expression node.
     *
     * @return pattern child expressions or empty list if there are no child expressions
     */
    public List<PatternExpr> getChildren();

    /**
     * Sets the list of pattern sub-expressions (child expressions) to the current pattern expression node.
     *
     * @param children pattern child expressions or empty list if there are no child expressions
     */
    public void setChildren(List<PatternExpr> children);

    /**
     * Returns the precedence.
     *
     * @return precedence
     */
    public PatternExprPrecedenceEnum getPrecedence();

    /**
     * Renders the pattern expression and all it's child expressions, in full tree depth, as a string in
     * language syntax.
     *
     * @param writer           is the output to use
     * @param parentPrecedence precedence
     * @param formatter        formatter
     */
    public void toEPL(StringWriter writer, PatternExprPrecedenceEnum parentPrecedence, EPStatementFormatter formatter);

    /**
     * Returns the id for the pattern expression, for use by tools.
     *
     * @return id
     */
    public String getTreeObjectName();

    /**
     * Sets and id for the pattern expression, for use by tools.
     *
     * @param objectName id
     */
    public void setTreeObjectName(String objectName);
}
