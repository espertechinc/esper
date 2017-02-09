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
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor that collects {@link com.espertech.esper.epl.expression.subquery.ExprSubselectNode} instances only
 * directly under alias expressions, and declared expressions, stopping at nested declared expressions.
 */
public class ExprNodeSubselectDeclaredNoTraverseVisitor implements ExprNodeVisitorWithParent {
    private final ExprDeclaredNode declaration;
    private final List<ExprSubselectNode> subselects;

    /**
     * Ctor.
     *
     * @param declaration declare node
     */
    public ExprNodeSubselectDeclaredNoTraverseVisitor(ExprDeclaredNode declaration) {
        this.declaration = declaration;
        subselects = new ArrayList<ExprSubselectNode>(1);
    }

    public void reset() {
        subselects.clear();
    }

    /**
     * Returns a list of lookup expression nodes.
     *
     * @return lookup nodes
     */
    public List<ExprSubselectNode> getSubselects() {
        return subselects;
    }

    public boolean isVisit(ExprNode exprNode) {
        return exprNode != declaration && !(exprNode instanceof ExprDeclaredNode);
    }

    public void visit(ExprNode exprNode, ExprNode parentExprNode) {

        if (!(exprNode instanceof ExprSubselectNode)) {
            return;
        }

        ExprSubselectNode subselectNode = (ExprSubselectNode) exprNode;
        subselects.add(subselectNode);
    }
}
