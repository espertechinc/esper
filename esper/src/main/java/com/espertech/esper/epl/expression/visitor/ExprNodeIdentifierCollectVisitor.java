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

import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Visitor that collects event property identifier information under expression nodes.
 */
public class ExprNodeIdentifierCollectVisitor implements ExprNodeVisitor {
    private final List<ExprIdentNode> exprProperties;

    /**
     * Ctor.
     */
    public ExprNodeIdentifierCollectVisitor() {
        this.exprProperties = new LinkedList<ExprIdentNode>();
    }

    public boolean isVisit(ExprNode exprNode) {
        return true;
    }

    /**
     * Returns list of event property stream numbers and names that uniquely identify which
     * property is from whcih stream, and the name of each.
     *
     * @return list of event property statement-unique info
     */
    public List<ExprIdentNode> getExprProperties() {
        return exprProperties;
    }

    public Set<Integer> getStreamsRequired() {
        Set<Integer> streams = new HashSet<Integer>();
        for (ExprIdentNode node : exprProperties) {
            streams.add(node.getStreamId());
        }
        return streams;
    }

    public void visit(ExprNode exprNode) {
        if (!(exprNode instanceof ExprIdentNode)) {
            return;
        }

        ExprIdentNode identNode = (ExprIdentNode) exprNode;
        exprProperties.add(identNode);
    }
}