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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class EnumDistinctOfHelper {

    public static void forEachBlock(CodegenBlock block, CodegenExpression eval, Class innerType) {
        if (!innerType.isArray()) {
            block.declareVar(innerType, "comparable", eval);
        } else {
            Class arrayMK = MultiKeyPlanner.getMKClassForComponentType(innerType.getComponentType());
            block.declareVar(arrayMK, "comparable", newInstance(arrayMK, eval));
        }
        block.ifCondition(not(exprDotMethod(ref("distinct"), "containsKey", ref("comparable"))))
            .expression(exprDotMethod(ref("distinct"), "put", ref("comparable"), ref("next")))
            .blockEnd();
    }
}
