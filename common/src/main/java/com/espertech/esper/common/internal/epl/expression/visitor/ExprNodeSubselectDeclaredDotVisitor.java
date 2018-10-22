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
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredNode;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNode;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor that collects subqueries, declared-expression and chained-dot.
 */
public class ExprNodeSubselectDeclaredDotVisitor implements ExprNodeVisitor {
    private final List<ExprSubselectNode> subselects;
    private final List<ExprDotNode> chainedExpressionsDot;
    private final List<ExprDeclaredNode> declaredExpressions;

    /**
     * Ctor.
     */
    public ExprNodeSubselectDeclaredDotVisitor() {
        subselects = new ArrayList<>(4);
        chainedExpressionsDot = new ArrayList<>(4);
        declaredExpressions = new ArrayList<>(1);
    }

    public void reset() {
        subselects.clear();
        chainedExpressionsDot.clear();
        declaredExpressions.clear();
    }

    /**
     * Returns a list of lookup expression nodes.
     *
     * @return lookup nodes
     */
    public List<ExprSubselectNode> getSubselects() {
        return subselects;
    }

    public List<ExprDotNode> getChainedExpressionsDot() {
        return chainedExpressionsDot;
    }

    public List<ExprDeclaredNode> getDeclaredExpressions() {
        return declaredExpressions;
    }

    public boolean isVisit(ExprNode exprNode) {
        return true;
    }

    public void visit(ExprNode exprNode) {

        if (exprNode instanceof ExprDotNode) {
            chainedExpressionsDot.add((ExprDotNode) exprNode);
        }

        if (exprNode instanceof ExprDeclaredNode) {
            declaredExpressions.add((ExprDeclaredNode) exprNode);
        }
        if (exprNode instanceof ExprSubselectNode) {
            ExprSubselectNode subselectNode = (ExprSubselectNode) exprNode;
            subselects.add(subselectNode);
        }
    }
}
