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
package com.espertech.esper.common.internal.epl.expression.dot.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.rettype.EPChainableType;
import com.espertech.esper.common.internal.rettype.EPChainableTypeHelper;

import java.lang.reflect.Array;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational.CodegenRelational.LE;

public class ExprDotForgeGetArrayEval implements ExprDotEval {
    private final ExprDotForgeGetArray forge;
    private final ExprEvaluator indexExpression;

    public ExprDotForgeGetArrayEval(ExprDotForgeGetArray forge, ExprEvaluator indexExpression) {
        this.forge = forge;
        this.indexExpression = indexExpression;
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }

        Object index = indexExpression.evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        if (index == null) {
            return null;
        }
        if (!(index instanceof Integer)) {
            return null;
        }
        int indexNum = (Integer) index;

        if (Array.getLength(target) <= indexNum) {
            return null;
        }
        return Array.get(target, indexNum);
    }

    public EPChainableType getTypeInfo() {
        return forge.getTypeInfo();
    }

    public ExprDotForge getDotForge() {
        return forge;
    }

    public static CodegenExpression codegen(ExprDotForgeGetArray forge, CodegenExpression inner, EPTypeClass innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        EPType returnType = EPChainableTypeHelper.getNormalizedEPType(forge.getTypeInfo());
        if (returnType == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        CodegenMethod methodNode = codegenMethodScope.makeChild((EPTypeClass) returnType, ExprDotForgeGetArrayEval.class, codegenClassScope).addParam(innerType, "target");

        CodegenBlock block = methodNode.getBlock();
        if (!innerType.getType().isPrimitive()) {
            block.ifRefNullReturnNull("target");
        }
        EPTypeClass targetType = EPChainableTypeHelper.getCodegenReturnType(forge.getTypeInfo());
        block.declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "index", forge.getIndexExpression().evaluateCodegen(EPTypePremade.INTEGERPRIMITIVE.getEPType(), methodNode, exprSymbol, codegenClassScope))
                .ifCondition(relational(arrayLength(ref("target")), LE, ref("index")))
                .blockReturn(constantNull())
                .methodReturn(CodegenLegoCast.castSafeFromObjectType(targetType, arrayAtIndex(ref("target"), ref("index"))));
        return localMethod(methodNode, inner);
    }
}
