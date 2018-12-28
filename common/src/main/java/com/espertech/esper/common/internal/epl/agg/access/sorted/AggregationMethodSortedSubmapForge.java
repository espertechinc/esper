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
package com.espertech.esper.common.internal.epl.agg.access.sorted;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionAggregationMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationMethodForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationMethodSortedSubmapForge implements AggregationMethodForge {

    private final ExprNode fromKey;
    private final ExprNode fromInclusive;
    private final ExprNode toKey;
    private final ExprNode toInclusive;
    private final Class underlyingClass;
    private final AggregationMethodSortedEnum aggMethod;
    private final Class resultType;

    public AggregationMethodSortedSubmapForge(ExprNode fromKey, ExprNode fromInclusive, ExprNode toKey, ExprNode toInclusive, Class underlyingClass, AggregationMethodSortedEnum aggMethod, Class resultType) {
        this.fromKey = fromKey;
        this.fromInclusive = fromInclusive;
        this.toKey = toKey;
        this.toInclusive = toInclusive;
        this.underlyingClass = underlyingClass;
        this.aggMethod = aggMethod;
        this.resultType = resultType;
    }

    public CodegenExpression codegenCreateReader(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationMultiFunctionAggregationMethod.class, this.getClass(), classScope);
        method.getBlock()
            .declareVar(ExprEvaluator.class, "fromKeyEval", ExprNodeUtilityCodegen.codegenEvaluator(fromKey.getForge(), method, this.getClass(), classScope))
            .declareVar(ExprEvaluator.class, "fromInclusiveEval", ExprNodeUtilityCodegen.codegenEvaluator(fromInclusive.getForge(), method, this.getClass(), classScope))
            .declareVar(ExprEvaluator.class, "toKeyEval", ExprNodeUtilityCodegen.codegenEvaluator(toKey.getForge(), method, this.getClass(), classScope))
            .declareVar(ExprEvaluator.class, "toInclusiveEval", ExprNodeUtilityCodegen.codegenEvaluator(toInclusive.getForge(), method, this.getClass(), classScope))
            .methodReturn(staticMethod(AggregationMethodSortedSubmapFactory.class, "makeSortedAggregationSubmap",
                ref("fromKeyEval"), ref("fromInclusiveEval"), ref("toKeyEval"), ref("toInclusiveEval"),
                enumValue(AggregationMethodSortedEnum.class, aggMethod.name()), constant(underlyingClass)));
        return localMethod(method);
    }

    public Class getResultType() {
        return resultType;
    }
}
