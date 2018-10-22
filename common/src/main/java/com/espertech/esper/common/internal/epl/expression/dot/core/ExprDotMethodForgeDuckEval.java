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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.MethodResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotMethodForgeDuckEval implements ExprDotEval {
    private static final Logger log = LoggerFactory.getLogger(ExprDotMethodForgeDuckEval.class);

    private final ExprDotMethodForgeDuck forge;
    private final ExprEvaluator[] parameters;

    private Map<Class, Method> cache;

    ExprDotMethodForgeDuckEval(ExprDotMethodForgeDuck forge, ExprEvaluator[] parameters) {
        this.forge = forge;
        this.parameters = parameters;
        cache = new HashMap<>();
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }

        Method method = dotMethodDuckGetMethod(target.getClass(), cache, forge.getMethodName(), forge.getParameterTypes(), new boolean[forge.getParameters().length]);
        if (method == null) {
            return null;
        }

        Object[] args = new Object[parameters.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = parameters[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        return dotMethodDuckInvokeMethod(method, target, args, forge.getStatementName());
    }

    public static CodegenExpression codegen(ExprDotMethodForgeDuck forge, CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression mCache = codegenClassScope.addFieldUnshared(true, Map.class, newInstance(HashMap.class));
        CodegenMethod methodNode = codegenMethodScope.makeChild(Object.class, ExprDotMethodForgeDuckEval.class, codegenClassScope).addParam(innerType, "target");

        CodegenBlock block = methodNode.getBlock()
                .ifRefNullReturnNull("target")
                .declareVar(Method.class, "method", staticMethod(ExprDotMethodForgeDuckEval.class, "dotMethodDuckGetMethod", exprDotMethod(ref("target"), "getClass"),
                        mCache, constant(forge.getMethodName()), constant(forge.getParameterTypes()), constant(new boolean[forge.getParameterTypes().length])))
                .ifRefNullReturnNull("method")
                .declareVar(Object[].class, "args", newArrayByLength(Object.class, constant(forge.getParameters().length)));
        for (int i = 0; i < forge.getParameters().length; i++) {
            block.assignArrayElement("args", constant(i), forge.getParameters()[i].evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope));
        }
        CodegenExpression statementName = exprDotMethod(exprSymbol.getAddExprEvalCtx(methodNode), "getStatementName");
        block.methodReturn(staticMethod(ExprDotMethodForgeDuckEval.class, "dotMethodDuckInvokeMethod", ref("method"), ref("target"), ref("args"), statementName));
        return localMethod(methodNode, inner);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param targetClass clazz
     * @param cache       cache
     * @param methodName  name
     * @param paramTypes  params
     * @param allFalse    all-false boolean same size as params
     * @return method
     */
    public static Method dotMethodDuckGetMethod(Class targetClass, Map<Class, Method> cache, String methodName, Class[] paramTypes, boolean[] allFalse) {
        Method method;
        synchronized (cache) {
            if (cache.containsKey(targetClass)) {
                method = cache.get(targetClass);
            } else {
                method = getMethod(targetClass, methodName, paramTypes, allFalse);
                cache.put(targetClass, method);
            }
        }
        return method;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param method        method
     * @param target        target
     * @param args          args
     * @param statementName statementName
     * @return result
     */
    public static Object dotMethodDuckInvokeMethod(Method method, Object target, Object[] args, String statementName) {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException | IllegalAccessException e) {
            String message = JavaClassHelper.getMessageInvocationTarget(statementName, method.getName(), method.getParameterTypes(), target.getClass().getName(), args, e);
            log.error(message, e);
        }
        return null;
    }

    private static Method getMethod(Class clazz, String methodName, Class[] paramTypes, boolean[] allFalse) {
        try {
            return MethodResolver.resolveMethod(clazz, methodName, paramTypes, true, allFalse, allFalse);
        } catch (Exception e) {
            log.debug("Not resolved for class '" + clazz.getName() + "' method '" + methodName + "'");
        }
        return null;
    }

    public EPType getTypeInfo() {
        return forge.getTypeInfo();
    }

    public ExprDotForge getDotForge() {
        return forge;
    }
}
