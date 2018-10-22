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
import com.espertech.esper.common.internal.epl.expression.core.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class QueryGraphValueEntryHashKeyedForgeExpr extends QueryGraphValueEntryHashKeyedForge {
    private final boolean requiresKey;

    public QueryGraphValueEntryHashKeyedForgeExpr(ExprNode keyExpr, boolean requiresKey) {
        super(keyExpr);
        this.requiresKey = requiresKey;
    }

    public boolean isRequiresKey() {
        return requiresKey;
    }

    public boolean isConstant() {
        return ExprNodeUtilityQuery.isConstant(super.getKeyExpr());
    }

    public String toQueryPlan() {
        return ExprNodeUtilityPrint.toExpressionStringMinPrecedenceSafe(getKeyExpr());
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbol, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(QueryGraphValueEntryHashKeyedExpr.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ExprEvaluator.class, "expression", ExprNodeUtilityCodegen.codegenEvaluatorNoCoerce(getKeyExpr().getForge(), method, this.getClass(), classScope))
                .methodReturn(newInstance(QueryGraphValueEntryHashKeyedExpr.class,
                        ref("expression"), constant(requiresKey)));
        return localMethod(method);
    }
}

