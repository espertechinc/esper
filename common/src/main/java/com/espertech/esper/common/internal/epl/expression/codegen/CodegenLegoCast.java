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


import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class CodegenLegoCast {
    public static CodegenExpression castSafeFromObjectType(EPTypeClass targetType, CodegenExpression value) {
        if (targetType == null) {
            return constantNull();
        }
        if (targetType.getType() == Object.class) {
            return value;
        }
        if (JavaClassHelper.isTypeVoid(targetType)) {
            throw new IllegalArgumentException("Invalid void target type for cast");
        }
        if (targetType.getType().isPrimitive()) {
            return cast(JavaClassHelper.getBoxedType(targetType), value);
        }
        return cast(targetType, value);
    }

    public static CodegenExpression castSafeFromObjectType(EPType targetType, CodegenExpression value) {
        if (targetType == null || targetType == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        EPTypeClass typeClass = (EPTypeClass) targetType;
        if (typeClass.getType() == Object.class) {
            return value;
        }
        if (JavaClassHelper.isTypeVoid(typeClass)) {
            throw new IllegalArgumentException("Invalid void target type for cast");
        }
        if (typeClass.getType().isPrimitive()) {
            return cast(JavaClassHelper.getBoxedType(typeClass), value);
        }
        return cast(typeClass, value);
    }

    public static void asDoubleNullReturnNull(CodegenBlock block, String variable, ExprForge forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        EPType type = forge.getEvaluationType();
        if (type == EPTypeNull.INSTANCE) {
            block.declareVar(EPTypePremade.DOUBLEPRIMITIVE.getEPType(), variable, constant(0));
        } else {
            EPTypeClass typeClass = (EPTypeClass) type;
            if (typeClass.getType() == double.class) {
                block.declareVar(typeClass, variable, forge.evaluateCodegen(typeClass, codegenMethodScope, exprSymbol, codegenClassScope));
            } else {
                String holder = variable + "_";
                block.declareVar(typeClass, holder, forge.evaluateCodegen(typeClass, codegenMethodScope, exprSymbol, codegenClassScope));
                if (!typeClass.getType().isPrimitive()) {
                    block.ifRefNullReturnNull(holder);
                }
                block.declareVar(EPTypePremade.DOUBLEPRIMITIVE.getEPType(), variable, SimpleNumberCoercerFactory.SimpleNumberCoercerDouble.codegenDouble(ref(holder), typeClass));
            }
        }
    }
}
