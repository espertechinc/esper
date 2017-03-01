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
import com.espertech.esper.epl.expression.dot.ExprDotNode;
import com.espertech.esper.epl.variable.VariableService;

import java.util.HashSet;
import java.util.Set;

/**
 * Visitor for expression node trees that determines if the expressions within contain a variable.
 */
public class ExprNodeVariableVisitor implements ExprNodeVisitor {
    private final VariableService variableService;
    private Set<String> variableNames;

    public ExprNodeVariableVisitor(VariableService variableService) {
        this.variableService = variableService;
    }

    public boolean isVisit(ExprNode exprNode) {
        return true;
    }

    /**
     * Returns true if the visitor finds a variable value.
     *
     * @return true for variable present in expression
     */
    public boolean isHasVariables() {
        return variableNames != null && !variableNames.isEmpty();
    }

    public void visit(ExprNode exprNode) {
        if (exprNode instanceof ExprDotNode) {
            ExprDotNode exprDotNode = (ExprDotNode) exprNode;
            String variableName = exprDotNode.isVariableOpGetName(variableService);
            if (variableName != null) {
                addVariableName(variableName);
            }
        }
        if (exprNode instanceof ExprVariableNode) {
            ExprVariableNode variableNode = (ExprVariableNode) exprNode;
            addVariableName(variableNode.getVariableName());
        }
    }

    /**
     * Returns the set of variable names encoountered.
     *
     * @return variable names
     */
    public Set<String> getVariableNames() {
        return variableNames;
    }

    private void addVariableName(String name) {
        if (variableNames == null) {
            variableNames = new HashSet<String>();
        }
        variableNames.add(name);
    }
}
