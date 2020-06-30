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
package com.espertech.esper.common.internal.epl.expression.ops;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoCast;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.reflect.Array;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Represents the "new Class[dim][dim]" operator in an expression tree.
 */
public class ExprNewInstanceNodeArrayForgeEval implements ExprEvaluator {

    private final static String NULL_MSG = "new-array received a null value for dimension";

    private final ExprNewInstanceNodeArrayForge forge;

    public ExprNewInstanceNodeArrayForgeEval(ExprNewInstanceNodeArrayForge forge) {
        this.forge = forge;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (forge.getParent().isArrayInitializedByExpr()) {
            return forge.getParent().getChildNodes()[0].getForge().getExprEvaluator().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }
        ExprNode[] children = forge.getParent().getChildNodes();
        int[] dimensions = new int[children.length];
        for (int i = 0; i < children.length; i++) {
            Integer size = (Integer) forge.getParent().getChildNodes()[i].getForge().getExprEvaluator().evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            if (size == null) {
                throw new EPException(NULL_MSG);
            }
            dimensions[i] = size;
        }
        EPTypeClass target = forge.getTargetClass();
        if (forge.getParent().getNumArrayDimensions() > dimensions.length) {
            target = JavaClassHelper.getArrayType(forge.getTargetClass(), forge.getParent().getNumArrayDimensions() - 1);
        }
        return Array.newInstance(target.getType(), dimensions);
    }

    public static CodegenExpression evaluateCodegen(EPTypeClass requiredType, ExprNewInstanceNodeArrayForge forge, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        if (forge.getParent().isArrayInitializedByExpr()) {
            return forge.getParent().getChildNodes()[0].getForge().evaluateCodegen(requiredType, parent, symbols, classScope);
        }

        CodegenMethod method = parent.makeChild(requiredType, ExprNewInstanceNodeArrayForgeEval.class, classScope);
        ExprNode[] dimensions = forge.getParent().getChildNodes();

        CodegenExpression[] dimValue = new CodegenExpression[dimensions.length];
        for (int i = 0; i < dimensions.length; i++) {
            ExprForge dimForge = forge.getParent().getChildNodes()[i].getForge();
            CodegenExpression dimExpr = dimForge.evaluateCodegen(EPTypePremade.INTEGERBOXED.getEPType(), method, symbols, classScope);
            if (dimForge.getForgeConstantType() == ExprForgeConstantType.COMPILETIMECONST) {
                dimValue[i] = dimExpr;
            } else {
                String name = "dim" + i;
                method.getBlock()
                    .declareVar(EPTypePremade.INTEGERBOXED.getEPType(), name, dimExpr)
                    .ifRefNull(name).blockThrow(newInstance(EPException.EPTYPE, constant(NULL_MSG)));
                dimValue[i] = ref(name);
            }
        }

        int numDimensions = forge.getParent().getNumArrayDimensions();
        if (numDimensions == 0 || numDimensions > 2) {
            throw new IllegalStateException("Only handles one- and two-dimensional arrays");
        }

        CodegenExpression make;
        if (dimValue.length == 1) {
            EPTypeClass target = JavaClassHelper.getArrayType(forge.getTargetClass(), forge.getParent().getNumArrayDimensions() - 1);
            make = newArrayByLength(target, dimValue[0]);
        } else {
            CodegenExpression[] params = new CodegenExpression[dimValue.length + 1];
            params[0] = clazz(forge.getTargetClass().getType());
            System.arraycopy(dimValue, 0, params, 1, dimValue.length);
            make = staticMethod(Array.class, "newInstance", params);
        }
        method.getBlock().methodReturn(CodegenLegoCast.castSafeFromObjectType(requiredType, make));
        return localMethod(method);
    }
}
