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

public class AggregationMethodLinearWindowForge implements AggregationMethodForge {
    private final Class arrayType;
    private final ExprNode optionalEvaluator;

    public AggregationMethodLinearWindowForge(Class arrayType, ExprNode optionalEvaluator) {
        this.arrayType = arrayType;
        this.optionalEvaluator = optionalEvaluator;
    }

    public Class getResultType() {
        return arrayType;
    }

    public CodegenExpression codegenCreateReader(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(AggregationMethodLinearWindow.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(AggregationMethodLinearWindow.class, "strat", newInstance(AggregationMethodLinearWindow.class))
                .exprDotMethod(ref("strat"), "setComponentType", constant(arrayType.getComponentType()))
                .exprDotMethod(ref("strat"), "setOptionalEvaluator", optionalEvaluator == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(optionalEvaluator.getForge(), method, this.getClass(), classScope))
                .methodReturn(ref("strat"));
        return localMethod(method);
    }
}
