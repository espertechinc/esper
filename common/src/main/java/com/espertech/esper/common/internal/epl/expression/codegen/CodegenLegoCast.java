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
package com.espertech.esper.common.internal.epl.expression.codegen;


import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class CodegenLegoCast {
    public static CodegenExpression castSafeFromObjectType(Class targetType, CodegenExpression value) {
        if (targetType == null) {
            return constantNull();
        }
        if (targetType == Object.class) {
            return value;
        }
        if (targetType == void.class) {
            throw new IllegalArgumentException("Invalid void target type for cast");
        }
        if (targetType.isPrimitive()) {
            return cast(JavaClassHelper.getBoxedType(targetType), value);
        }
        return cast(targetType, value);
    }

    public static void asDoubleNullReturnNull(CodegenBlock block, String variable, ExprForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        Class type = forge.getEvaluationType();
        if (type == double.class) {
            block.declareVar(type, variable, forge.evaluateCodegen(type, codegenMethodScope, exprSymbol, codegenClassScope));
            return;
        }

        String holder = variable + "_";
        block.declareVar(type, holder, forge.evaluateCodegen(type, codegenMethodScope, exprSymbol, codegenClassScope));
        if (!type.isPrimitive()) {
            block.ifRefNullReturnNull(holder);
        }
        block.declareVar(double.class, variable, SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(ref(holder), type));
    }
}
