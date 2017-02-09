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

import com.espertech.esper.epl.declexpr.ExprDeclaredNode;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor that collects {@link ExprDeclaredNode} instances.
 */
public class ExprNodeDeclaredVisitor implements ExprNodeVisitor {
    private final List<ExprDeclaredNode> declaredExpressions;

    /**
     * Ctor.
     */
    public ExprNodeDeclaredVisitor() {
        declaredExpressions = new ArrayList<ExprDeclaredNode>(1);
    }

    public void reset() {
        declaredExpressions.clear();
    }

    public List<ExprDeclaredNode> getDeclaredExpressions() {
        return declaredExpressions;
    }

    public boolean isVisit(ExprNode exprNode) {
        return true;
    }

    public void visit(ExprNode exprNode) {

        if (exprNode instanceof ExprDeclaredNode) {
            declaredExpressions.add((ExprDeclaredNode) exprNode);
        }
    }

    public void clear() {
        declaredExpressions.clear();
    }
}
