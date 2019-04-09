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
package com.espertech.esper.common.internal.epl.enummethod.eval;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;

public class EnumDistinctScalarForge extends EnumForgeBase implements EnumForge, EnumEval {

    private final Class fieldType;

    public EnumDistinctScalarForge(int streamCountIncoming, Class fieldType) {
        super(streamCountIncoming);
        this.fieldType = fieldType;
    }

    public EnumEval getEnumEvaluator() {
        return this;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection enumcoll, boolean isNewData, ExprEvaluatorContext context) {
        if (enumcoll.size() <= 1) {
            return enumcoll;
        }

        if (enumcoll instanceof Set) {
            return enumcoll;
        }

        return new LinkedHashSet<Object>(enumcoll);
    }

    public CodegenExpression codegen(EnumForgeCodegenParams args, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod method = codegenMethodScope.makeChild(Collection.class, EnumDistinctScalarForge.class, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMS);

        method.getBlock()
            .ifCondition(relational(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size"), LE, constant(1)))
            .blockReturn(EnumForgeCodegenNames.REF_ENUMCOLL);

        if (fieldType == null || !fieldType.isArray()) {
            method.getBlock()
                .ifCondition(instanceOf(ref("enumcoll"), Set.class))
                .blockReturn(EnumForgeCodegenNames.REF_ENUMCOLL)
                .methodReturn(newInstance(LinkedHashSet.class, EnumForgeCodegenNames.REF_ENUMCOLL));
        } else {
            Class arrayMK = MultiKeyPlanner.getMKClassForComponentType(fieldType.getComponentType());
            method.getBlock().declareVar(Map.class, "distinct", newInstance(LinkedHashMap.class));
            CodegenBlock loop = method.getBlock().forEach(Object.class, "next", EnumForgeCodegenNames.REF_ENUMCOLL);
            {
                loop.declareVar(arrayMK, "comparable", newInstance(arrayMK, cast(fieldType, ref("next"))))
                    .expression(exprDotMethod(ref("distinct"), "put", ref("comparable"), ref("next")));
            }
            method.getBlock().methodReturn(exprDotMethod(ref("distinct"), "values"));
        }

        return localMethod(method, args.getExpressions());
    }
}
