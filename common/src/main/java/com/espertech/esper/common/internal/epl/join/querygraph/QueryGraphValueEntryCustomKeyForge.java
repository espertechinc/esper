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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class QueryGraphValueEntryCustomKeyForge implements QueryGraphValueEntryForge {
    private final String operationName;
    private final ExprNode[] exprNodes;

    public QueryGraphValueEntryCustomKeyForge(String operationName, ExprNode[] exprNodes) {
        this.operationName = operationName;
        this.exprNodes = exprNodes;
    }

    public String getOperationName() {
        return operationName;
    }

    public ExprNode[] getExprNodes() {
        return exprNodes;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryGraphValueEntryCustomKeyForge that = (QueryGraphValueEntryCustomKeyForge) o;

        if (!operationName.equals(that.operationName)) return false;
        return ExprNodeUtilityCompare.deepEquals(exprNodes, that.exprNodes, true);
    }

    public int hashCode() {
        return operationName.hashCode();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(QueryGraphValueEntryCustomKey.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(QueryGraphValueEntryCustomKey.class, "key", newInstance(QueryGraphValueEntryCustomKey.class))
                .exprDotMethod(ref("key"), "setOperationName", constant(operationName))
                .exprDotMethod(ref("key"), "setExprNodes", ExprNodeUtilityCodegen.codegenEvaluators(exprNodes, method, this.getClass(), classScope))
                .exprDotMethod(ref("key"), "setExpressions", constant(ExprNodeUtilityPrint.toExpressionStringsMinPrecedence(exprNodes)))
                .methodReturn(ref("key"));
        return localMethod(method);
    }
}

