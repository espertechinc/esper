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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.distinctof;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenNames;
import com.espertech.esper.common.internal.epl.enummethod.codegen.EnumForgeCodegenParams;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumEval;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForge;
import com.espertech.esper.common.internal.epl.enummethod.eval.EnumForgeBasePlain;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;

public class EnumDistinctOfScalarNoParams extends EnumForgeBasePlain implements EnumForge, EnumEval {

    private final EPTypeClass fieldType;

    public EnumDistinctOfScalarNoParams(int streamCountIncoming, EPTypeClass fieldType) {
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
        CodegenMethod method = codegenMethodScope.makeChild(EPTypePremade.COLLECTION.getEPType(), EnumDistinctOfScalarNoParams.class, codegenClassScope).addParam(EnumForgeCodegenNames.PARAMSCOLLOBJ);

        method.getBlock()
            .ifCondition(relational(exprDotMethod(EnumForgeCodegenNames.REF_ENUMCOLL, "size"), LE, constant(1)))
            .blockReturn(EnumForgeCodegenNames.REF_ENUMCOLL);

        if (fieldType == null || !fieldType.getType().isArray()) {
            method.getBlock()
                .ifCondition(instanceOf(ref("enumcoll"), EPTypePremade.SET.getEPType()))
                .blockReturn(EnumForgeCodegenNames.REF_ENUMCOLL)
                .methodReturn(newInstance(EPTypePremade.LINKEDHASHSET.getEPType(), EnumForgeCodegenNames.REF_ENUMCOLL));
        } else {
            EPTypeClass componentType = JavaClassHelper.getArrayComponentType(fieldType);
            EPTypeClass arrayMK = MultiKeyPlanner.getMKClassForComponentType(componentType);
            method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "distinct", newInstance(EPTypePremade.LINKEDHASHMAP.getEPType()));
            CodegenBlock loop = method.getBlock().forEach(EPTypePremade.OBJECT.getEPType(), "next", EnumForgeCodegenNames.REF_ENUMCOLL);
            {
                loop.declareVar(arrayMK, "comparable", newInstance(arrayMK, cast(fieldType, ref("next"))))
                    .expression(exprDotMethod(ref("distinct"), "put", ref("comparable"), ref("next")));
            }
            method.getBlock().methodReturn(exprDotMethod(ref("distinct"), "values"));
        }

        return localMethod(method, args.getExpressions());
    }
}
