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
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor that collects expression nodes that require view resources.
 */
public class ExprNodeViewResourceVisitor implements ExprNodeVisitor {
    private final List<ExprNode> exprNodes;

    /**
     * Ctor.
     */
    public ExprNodeViewResourceVisitor() {
        exprNodes = new ArrayList<ExprNode>();
    }

    public boolean isVisit(ExprNode exprNode) {
        return true;
    }

    /**
     * Returns the list of expression nodes requiring view resources.
     *
     * @return expr nodes such as 'prior' or 'prev'
     */
    public List<ExprNode> getExprNodes() {
        return exprNodes;
    }

    public void visit(ExprNode exprNode) {
        if (exprNode instanceof ExprPreviousNode || exprNode instanceof ExprPriorNode) {
            exprNodes.add(exprNode);
        }
    }
}
