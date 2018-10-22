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
package com.espertech.esper.common.internal.epl.agg.access.countminsketch;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregationAgentForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationAgentCountMinSketchForge implements AggregationAgentForge {

    private final ExprForge stringEvaluator;
    private final ExprForge optionalFilterForge;

    public AggregationAgentCountMinSketchForge(ExprForge stringEvaluator, ExprForge optionalFilterForge) {
        this.stringEvaluator = stringEvaluator;
        this.optionalFilterForge = optionalFilterForge;
    }

    public ExprForge getOptionalFilter() {
        return optionalFilterForge;
    }

    public CodegenExpression make(CodegenMethod parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationAgentCountMinSketch.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(AggregationAgentCountMinSketch.class, "cms", newInstance(AggregationAgentCountMinSketch.class))
                .exprDotMethod(ref("cms"), "setStringEval", ExprNodeUtilityCodegen.codegenEvaluator(stringEvaluator, method, this.getClass(), classScope))
                .exprDotMethod(ref("cms"), "setOptionalFilterEval", optionalFilterForge == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(optionalFilterForge, method, this.getClass(), classScope))
                .methodReturn(ref("cms"));
        return localMethod(method);
    }
}
