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
package com.espertech.esper.common.internal.epl.agg.access.linear;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.agg.core.AggregationMethodForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AggregationMethodLinearFirstLastIndexForge implements AggregationMethodForge {
    private final Class underlyingType;
    private final AggregationAccessorLinearType accessType;
    private final Integer optionalConstant;
    private final ExprNode optionalIndexEval;

    public AggregationMethodLinearFirstLastIndexForge(Class underlyingType, AggregationAccessorLinearType accessType, Integer optionalConstant, ExprNode optionalIndexEval) {
        this.underlyingType = underlyingType;
        this.accessType = accessType;
        this.optionalConstant = optionalConstant;
        this.optionalIndexEval = optionalIndexEval;
    }

    public Class getResultType() {
        return underlyingType;
    }

    public CodegenExpression codegenCreateReader(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationMethodLinearFirstLastIndex.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(AggregationMethodLinearFirstLastIndex.class, "strat", newInstance(AggregationMethodLinearFirstLastIndex.class))
                .exprDotMethod(ref("strat"), "setAccessType", constant(accessType))
                .exprDotMethod(ref("strat"), "setOptionalConstIndex", constant(optionalConstant))
                .exprDotMethod(ref("strat"), "setOptionalIndexEval", optionalIndexEval == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(optionalIndexEval.getForge(), method, this.getClass(), classScope))
                .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
