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


import com.espertech.esper.common.internal.epl.enummethod.dot.ExprLambdaGoesNode;
import com.espertech.esper.common.internal.epl.expression.agg.base.ExprAggregateNode;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExprNodeIdentifierAndStreamRefVisitor implements ExprNodeVisitor {
    private final boolean isVisitAggregateNodes;
    private final boolean isVisitDeclaredExprParams;
    private final boolean isVisitDeclaredExprBody;
    private List<ExprNodePropOrStreamDesc> refs;
    private boolean hasWildcardOrStreamAlias;

    public ExprNodeIdentifierAndStreamRefVisitor(boolean isVisitAggregateNodes) {
        this(isVisitAggregateNodes, false, true);
    }

    public ExprNodeIdentifierAndStreamRefVisitor(boolean isVisitAggregateNodes, boolean isVisitDeclaredExprParams, boolean isVisitDeclaredExprBody) {
        this.isVisitAggregateNodes = isVisitAggregateNodes;
        this.isVisitDeclaredExprParams = isVisitDeclaredExprParams;
        this.isVisitDeclaredExprBody = isVisitDeclaredExprBody;
    }

    public boolean isVisit(ExprNode exprNode) {
        if (exprNode instanceof ExprLambdaGoesNode) {
            return false;
        }
        if (!isVisitDeclaredExprBody && exprNode instanceof ExprDeclaredNode) {
            return false;
        }
        if (isVisitAggregateNodes) {
            return true;
        }
        return !(exprNode instanceof ExprAggregateNode);
    }

    public List<ExprNodePropOrStreamDesc> getRefs() {
        if (refs == null) {
            return Collections.emptyList();
        }
        return refs;
    }

    public void visit(ExprNode exprNode) {
        if (exprNode instanceof ExprIdentNode) {
            ExprIdentNode identNode = (ExprIdentNode) exprNode;
            int streamId = identNode.getStreamId();
            String propertyName = identNode.getResolvedPropertyName();
            checkAllocatedRefs();
            refs.add(new ExprNodePropOrStreamPropDesc(streamId, propertyName));
        } else if (exprNode instanceof ExprStreamRefNode) {
            ExprStreamRefNode streamRefNode = (ExprStreamRefNode) exprNode;
            Integer stream = streamRefNode.getStreamReferencedIfAny();
            checkAllocatedRefs();
            if (stream != null) {
                refs.add(new ExprNodePropOrStreamExprDesc(stream, streamRefNode));
            }

            if (exprNode instanceof ExprWildcard || exprNode instanceof ExprStreamUnderlyingNode) {
                hasWildcardOrStreamAlias = true;
            }
        }
    }

    public void reset() {
        if (refs != null) {
            refs.clear();
        }
    }

    public boolean isWalkDeclExprParam() {
        return isVisitDeclaredExprParams;
    }

    public boolean isHasWildcardOrStreamAlias() {
        return hasWildcardOrStreamAlias;
    }

    private void checkAllocatedRefs() {
        if (refs == null) {
            refs = new ArrayList<ExprNodePropOrStreamDesc>(4);
        }
    }
}
