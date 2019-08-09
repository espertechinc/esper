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
package com.espertech.esper.common.internal.epl.datetime.plugin;

import com.espertech.esper.common.client.hook.datetimemethod.DateTimeMethodMode;
import com.espertech.esper.common.client.hook.datetimemethod.DateTimeMethodModeStaticMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.util.MethodResolver;
import com.espertech.esper.common.internal.util.MethodResolverNoSuchMethodException;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class DTMPluginUtil {

    static void validateDTMStaticMethodAllowNull(Class inputType, DateTimeMethodMode mode, Class firstParameter, List<ExprNode> paramExpressions) throws ExprValidationException {
        if (mode == null) {
            if (inputType == firstParameter) {
                throw new ExprValidationException("Plugin datetime method does not provide a forge for input type " + inputType.getName());
            }
            return;
        }
        if (!(mode instanceof DateTimeMethodModeStaticMethod)) {
            throw new ExprValidationException("Unexpected plug-in datetime method mode implementation " + mode.getClass());
        }
        DateTimeMethodModeStaticMethod staticMethod = (DateTimeMethodModeStaticMethod) mode;
        Class[] params = new Class[paramExpressions.size() + 1];
        params[0] = firstParameter;
        for (int i = 0; i < paramExpressions.size(); i++) {
            params[i + 1] = paramExpressions.get(i).getForge().getEvaluationType();
        }
        try {
            MethodResolver.resolveMethod(staticMethod.getClazz(), staticMethod.getMethodName(), params, false, new boolean[params.length], new boolean[params.length]);
        } catch (MethodResolverNoSuchMethodException ex) {
            throw new ExprValidationException("Failed to find static method for date-time method extension: " + ex.getMessage(), ex);
        }
    }

    static CodegenExpression codegenPluginDTM(DateTimeMethodMode mode, Class returnedClass, Class firstParameterClass, CodegenExpression firstParameterExpression, List<ExprNode> paramExpressions, CodegenMethodScope parent, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        DateTimeMethodModeStaticMethod staticMethod = (DateTimeMethodModeStaticMethod) mode;
        CodegenMethod method = parent.makeChild(returnedClass, DTMPluginValueChangeForge.class, classScope).addParam(firstParameterClass, "dt");
        CodegenExpression[] params = new CodegenExpression[paramExpressions.size() + 1];
        params[0] = ref("dt");
        for (int i = 0; i < paramExpressions.size(); i++) {
            ExprForge forge = paramExpressions.get(i).getForge();
            params[i + 1] = forge.evaluateCodegen(forge.getEvaluationType(), method, symbols, classScope);
        }
        CodegenExpression callStatic = staticMethod(staticMethod.getClazz(), staticMethod.getMethodName(), params);
        if (returnedClass == void.class) {
            method.getBlock().expression(callStatic);
        } else {
            method.getBlock().methodReturn(callStatic);
        }
        return localMethod(method, firstParameterExpression);
    }
}
