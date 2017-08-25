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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Collection;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.GT;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionRelational.CodegenRelational.LT;

public class EnumMinMaxScalarForge extends EnumForgeBase implements EnumForge, EnumEval {

    private final boolean max;
    private final EPType resultType;

    public EnumMinMaxScalarForge(int streamCountIncoming, boolean max, EPType resultType) {
        super(streamCountIncoming);
        this.max = max;
        this.resultType = resultType;
    }

    public EnumEval getEnumEvaluator() {
        return this;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        Comparable minKey = null;

        for (Object next : enumcoll) {

            Object comparable = next;
            if (comparable == null) {
                continue;
            }

            if (minKey == null) {
                minKey = (Comparable) comparable;
            } else {
                if (max) {
                    if (minKey.compareTo(comparable) < 0) {
                        minKey = (Comparable) comparable;
                    }
                } else {
                    if (minKey.compareTo(comparable) > 0) {
                        minKey = (Comparable) comparable;
                    }
                }
            }
        }

        return minKey;
    }

    public CodegenExpression codegen(EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        Class innerTypeBoxed = JavaClassHelper.getBoxedType(EPTypeHelper.getCodegenReturnType(resultType));

        CodegenBlock block = codegenMethodScope.makeChild(innerTypeBoxed, EnumMinMaxEventsForgeEval.class, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS).getBlock()
                .declareVar(innerTypeBoxed, "minKey", constantNull());

        CodegenBlock forEach = block.forEach(Object.class, "value", EnumForgeCodegenNames.REF_ENUMCOLL)
            .ifRefNull("value").blockContinue();

        forEach.ifCondition(equalsNull(ref("minKey")))
                .assignRef("minKey", cast(innerTypeBoxed, ref("value")))
                .ifElse()
                .ifCondition(relational(exprDotMethod(ref("minKey"), "compareTo", ref("value")), max ? LT : GT, constant(0)))
                .assignRef("minKey", cast(innerTypeBoxed, ref("value")));

        CodegenMethodNode method = block.methodReturn(ref("minKey"));
        return localMethod(method, args.getExpressions());
    }
}
