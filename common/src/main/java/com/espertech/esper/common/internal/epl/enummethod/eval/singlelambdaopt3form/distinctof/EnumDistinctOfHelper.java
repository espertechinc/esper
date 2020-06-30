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

import com.espertech.esper.common.client.type.*;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class EnumDistinctOfHelper {

    public static void forEachBlock(CodegenBlock block, CodegenExpression eval, EPType innerType) {
        if (innerType == EPTypeNull.INSTANCE) {
            block.declareVar(EPTypePremade.OBJECT.getEPType(), "comparable", constantNull());
        } else {
            EPTypeClass innerClass = (EPTypeClass) innerType;
            if (!innerClass.getType().isArray()) {
                block.declareVar((EPTypeClass) innerType, "comparable", eval);
            } else {
                EPTypeClass component = JavaClassHelper.getArrayComponentType(innerClass);
                EPTypeClass arrayMK = MultiKeyPlanner.getMKClassForComponentType(component);
                block.declareVar(arrayMK, "comparable", newInstance(arrayMK, eval));
            }
        }
        block.ifCondition(not(exprDotMethod(ref("distinct"), "containsKey", ref("comparable"))))
            .expression(exprDotMethod(ref("distinct"), "put", ref("comparable"), ref("next")))
            .blockEnd();
    }
}
