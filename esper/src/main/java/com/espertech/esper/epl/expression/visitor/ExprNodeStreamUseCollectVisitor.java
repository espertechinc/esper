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
import com.espertech.esper.epl.expression.core.ExprStreamRefNode;

import java.util.ArrayList;
import java.util.List;

public class ExprNodeStreamUseCollectVisitor implements ExprNodeVisitor {
    private final List<ExprStreamRefNode> referenced = new ArrayList<ExprStreamRefNode>();

    public boolean isVisit(ExprNode exprNode) {
        return true;
    }

    public List<ExprStreamRefNode> getReferenced() {
        return referenced;
    }

    public void visit(ExprNode exprNode) {
        if (!(exprNode instanceof ExprStreamRefNode)) {
            return;
        }
        referenced.add((ExprStreamRefNode) exprNode);
    }
}