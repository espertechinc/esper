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
package com.espertech.esper.common.internal.epl.agg.access.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public class AggregationAgentDefaultWFilterForge implements AggregationAgentForge {
    private final ExprForge filterEval;

    public AggregationAgentDefaultWFilterForge(ExprForge filterEval) {
        this.filterEval = filterEval;
    }

    public CodegenExpression make(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return newInstance(AggregationAgentDefaultWFilter.class, ExprNodeUtilityCodegen.codegenEvaluator(filterEval, method, this.getClass(), classScope));
    }

    public ExprForge getFilterEval() {
        return filterEval;
    }

    public ExprForge getOptionalFilter() {
        return filterEval;
    }
}
