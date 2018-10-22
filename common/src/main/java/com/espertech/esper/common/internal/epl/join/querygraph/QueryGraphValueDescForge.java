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
package com.espertech.esper.common.internal.epl.join.querygraph;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class QueryGraphValueDescForge {
    private final ExprNode[] indexExprs;
    private final QueryGraphValueEntryForge entry;

    public QueryGraphValueDescForge(ExprNode[] indexExprs, QueryGraphValueEntryForge entry) {
        this.indexExprs = indexExprs;
        this.entry = entry;
    }

    public ExprNode[] getIndexExprs() {
        return indexExprs;
    }

    public QueryGraphValueEntryForge getEntry() {
        return entry;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        String[] indexes = new String[indexExprs.length];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = getSingleIdentNodeProp(indexExprs[i]);
        }
        return newInstance(QueryGraphValueDesc.class, constant(indexes), entry.make(parent, symbols, classScope));
    }

    private String getSingleIdentNodeProp(ExprNode indexExpr) {
        ExprIdentNode identNode = (ExprIdentNode) indexExpr;
        return identNode.getResolvedPropertyName();
    }
}

