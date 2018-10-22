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
package com.espertech.esper.common.internal.epl.expression.visitor;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNode;
import com.espertech.esper.common.internal.epl.expression.variable.ExprVariableNode;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolver;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableMetaData;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Visitor for expression node trees that determines if the expressions within contain a variable.
 */
public class ExprNodeVariableVisitor implements ExprNodeVisitor {
    private final VariableCompileTimeResolver variableCompileTimeResolver;
    private Map<String, VariableMetaData> variableNames;

    public ExprNodeVariableVisitor(VariableCompileTimeResolver variableCompileTimeResolver) {
        this.variableCompileTimeResolver = variableCompileTimeResolver;
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
            VariableMetaData metadata = exprDotNode.isVariableOpGetName(variableCompileTimeResolver);
            if (metadata != null) {
                addVariableName(metadata);
            }
        }
        if (exprNode instanceof ExprVariableNode) {
            ExprVariableNode variableNode = (ExprVariableNode) exprNode;
            VariableMetaData metadata = variableNode.getVariableMetadata();
            addVariableName(metadata);
        }
    }

    public Map<String, VariableMetaData> getVariableNames() {
        return variableNames;
    }

    /**
     * Returns the set of variable names encoountered.
     *
     * @return variable names
     */

    private void addVariableName(VariableMetaData meta) {
        if (variableNames == null) {
            variableNames = new LinkedHashMap<>();
        }
        variableNames.put(meta.getVariableName(), meta);
    }
}
