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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityPrint;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class QueryGraphValueEntryInKeywordSingleIdxForge implements QueryGraphValueEntryForge {
    private final ExprNode[] keyExprs;

    protected QueryGraphValueEntryInKeywordSingleIdxForge(ExprNode[] keyExprs) {
        this.keyExprs = keyExprs;
    }

    public ExprNode[] getKeyExprs() {
        return keyExprs;
    }

    public String toQueryPlan() {
        return "in-keyword single-indexed multiple key lookup " + ExprNodeUtilityPrint.toExpressionStringMinPrecedenceAsList(keyExprs);
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(QueryGraphValueEntryInKeywordSingleIdx.class, this.getClass(), classScope);
        method.getBlock()
            .declareVar(ExprEvaluator[].class, "expressions", ExprNodeUtilityCodegen.codegenEvaluators(keyExprs, method, this.getClass(), classScope))
            .methodReturn(newInstance(QueryGraphValueEntryInKeywordSingleIdx.class, ref("expressions")));
        return localMethod(method);
    }
}

