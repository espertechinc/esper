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
package com.espertech.esper.epl.expression.visitor;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprVariableNode;

import java.util.HashSet;
import java.util.Set;

/**
 * Visitor for expression node trees that determines if the expressions within contain a variable.
 */
public class ExprNodeVariableVisitor implements ExprNodeVisitor
{
    private boolean hasVariables;
    private Set<String> variableNames;

    public boolean isVisit(ExprNode exprNode)
    {
        return true;
    }

    /**
     * Returns true if the visitor finds a variable value.
     * @return true for variable present in expression
     */
    public boolean isHasVariables()
    {
        return hasVariables;
    }

    public void visit(ExprNode exprNode)
    {
        if (!(exprNode instanceof ExprVariableNode))
        {
            return;
        }
        hasVariables = true;

        ExprVariableNode variableNode = (ExprVariableNode) exprNode;
        if (variableNames == null)
        {
            variableNames = new HashSet<String>();
        }
        variableNames.add(variableNode.getVariableName());
    }

    /**
     * Returns the set of variable names encoountered.
     * @return variable names
     */
    public Set<String> getVariableNames()
    {
        return variableNames;
    }
}
