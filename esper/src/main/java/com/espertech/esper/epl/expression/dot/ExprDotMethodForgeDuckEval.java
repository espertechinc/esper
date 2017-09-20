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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.util.JavaClassHelper;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprDotMethodForgeDuckEval implements ExprDotEval {
    private static final Logger log = LoggerFactory.getLogger(ExprDotMethodForgeDuckEval.class);

    private final ExprDotMethodForgeDuck forge;
    private final ExprEvaluator[] parameters;

    private Map<Class, FastMethod> cache;

    ExprDotMethodForgeDuckEval(ExprDotMethodForgeDuck forge, ExprEvaluator[] parameters) {
        this.forge = forge;
        this.parameters = parameters;
        cache = new HashMap<>();
    }

    public Object evaluate(Object target, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (target == null) {
            return null;
        }

        FastMethod method = dotMethodDuckGetMethod(target.getClass(), cache, forge);
        if (method == null) {
            return null;
        }

        Object[] args = new Object[parameters.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = parameters[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        return dotMethodDuckInvokeMethod(method, target, args, forge);
    }

    public static CodegenExpression codegen(ExprDotMethodForgeDuck forge, CodegenExpression inner, Class innerType, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember mCache = codegenClassScope.makeAddMember(Map.class, new HashMap());
        CodegenMember mForge = codegenClassScope.makeAddMember(ExprDotMethodForgeDuck.class, forge);
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(Object.class, ExprDotMethodForgeDuckEval.class, codegenClassScope).addParam(innerType, "target");

        CodegenBlock block = methodNode.getBlock()
                .ifRefNullReturnNull("target")
                .declareVar(FastMethod.class, "method", staticMethod(ExprDotMethodForgeDuckEval.class, "dotMethodDuckGetMethod", exprDotMethod(ref("target"), "getClass"), member(mCache.getMemberId()), member(mForge.getMemberId())))
                .ifRefNullReturnNull("method")
                .declareVar(Object[].class, "args", newArrayByLength(Object.class, constant(forge.getParameters().length)));
        for (int i = 0; i < forge.getParameters().length; i++) {
            block.assignArrayElement("args", constant(i), forge.getParameters()[i].evaluateCodegen(Object.class, methodNode, exprSymbol, codegenClassScope));
        }
        block.methodReturn(staticMethod(ExprDotMethodForgeDuckEval.class, "dotMethodDuckInvokeMethod", ref("method"), ref("target"), ref("args"), member(mForge.getMemberId())));
        return localMethod(methodNode, inner);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param targetClass target
     * @param cache cache
     * @param forge forge
     * @return method
     */
    public static FastMethod dotMethodDuckGetMethod(Class targetClass, Map<Class, FastMethod> cache, ExprDotMethodForgeDuck forge) {
        FastMethod method;
        synchronized (cache) {
            if (cache.containsKey(targetClass)) {
                method = cache.get(targetClass);
            } else {
                method = getFastMethod(targetClass, forge);
                cache.put(targetClass, method);
            }
        }
        return method;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param method method
     * @param target target
     * @param args args
     * @param forge forge
     * @return result
     */
    public static Object dotMethodDuckInvokeMethod(FastMethod method, Object target, Object[] args, ExprDotMethodForgeDuck forge) {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            String message = JavaClassHelper.getMessageInvocationTarget(forge.getStatementName(), method.getJavaMethod(), target.getClass().getName(), args, e.getTargetException());
            log.error(message, e.getTargetException());
        }
        return null;
    }

    private static FastMethod getFastMethod(Class clazz, ExprDotMethodForgeDuck forge) {
        try {
            Method method = forge.getEngineImportService().resolveMethod(clazz, forge.getMethodName(), forge.getParameterTypes(), new boolean[forge.getParameterTypes().length], new boolean[forge.getParameterTypes().length]);
            FastClass declaringClass = FastClass.create(forge.getEngineImportService().getFastClassClassLoader(method.getDeclaringClass()), method.getDeclaringClass());
            return declaringClass.getMethod(method);
        } catch (Exception e) {
            log.debug("Not resolved for class '" + clazz.getName() + "' method '" + forge.getMethodName() + "'");
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
