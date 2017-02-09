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

import java.util.HashSet;
import java.util.Set;

public class ExprNodeStreamRequiredVisitor implements ExprNodeVisitor {
    private final Set<Integer> streams;

    public ExprNodeStreamRequiredVisitor() {
        this.streams = new HashSet<Integer>();
    }

    public boolean isVisit(ExprNode exprNode) {
        return true;
    }

    public Set<Integer> getStreamsRequired() {
        return streams;
    }

    public void visit(ExprNode exprNode) {
        if (exprNode instanceof ExprStreamRefNode) {
            ExprStreamRefNode streamRefNode = (ExprStreamRefNode) exprNode;
            Integer streamRef = streamRefNode.getStreamReferencedIfAny();
            if (streamRef != null) {
                streams.add(streamRef);
            }
        }
    }
}