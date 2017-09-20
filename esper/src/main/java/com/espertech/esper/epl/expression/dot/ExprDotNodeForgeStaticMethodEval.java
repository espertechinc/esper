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

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprDotNodeForgeStaticMethodEval implements ExprEvaluator, EventPropertyGetter {
    private static final Logger log = LoggerFactory.getLogger(ExprDotNodeForgeStaticMethodEval.class);
    public final static String METHOD_STATICMETHODEVALHANDLEINVOCATIONEXCEPTION = "staticMethodEvalHandleInvocationException";

    private final ExprDotNodeForgeStaticMethod forge;
    private final ExprEvaluator[] childEvals;
    private final ExprDotEval[] chainEval;

    private boolean isCachedResult;
    private Object cachedResult;

    public ExprDotNodeForgeStaticMethodEval(ExprDotNodeForgeStaticMethod forge, ExprEvaluator[] childEvals, ExprDotEval[] chainEval) {
        this.forge = forge;
        this.childEvals = childEvals;
        this.chainEval = chainEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprPlugInSingleRow(forge.getStaticMethod().getJavaMethod());
        }
        if (forge.isConstantParameters() && isCachedResult) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprPlugInSingleRow(cachedResult);
            }
            return cachedResult;
        }

        Object[] args = new Object[childEvals.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = childEvals[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        // The method is static so the object it is invoked on
        // can be null
        try {
            Object result = forge.getStaticMethod().invoke(forge.getTargetObject(), args);

            result = ExprDotNodeUtility.evaluateChainWithWrap(forge.getResultWrapLambda(), result, null, forge.getStaticMethod().getReturnType(), chainEval, forge.getChainForges(), eventsPerStream, isNewData, exprEvaluatorContext);

            if (forge.isConstantParameters()) {
                cachedResult = result;
                isCachedResult = true;
            }

            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprPlugInSingleRow(result);
            }
            return result;
        } catch (InvocationTargetException e) {
            staticMethodEvalHandleInvocationException(forge.getStatementName(), forge.getStaticMethod().getJavaMethod(), forge.getClassOrPropertyName(), args, e.getTargetException(), forge.isRethrowExceptions());
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprPlugInSingleRow(null);
        }
        return null;
    }

    public static CodegenExpression codegen(ExprDotNodeForgeStaticMethod forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMember isCachedMember = null;
        CodegenMember cachedResultMember = null;
        CodegenMember methodMember = codegenClassScope.makeAddMember(Method.class, forge.getStaticMethod().getJavaMethod());
        if (forge.isConstantParameters()) {
            isCachedMember = codegenClassScope.makeAddMember(AtomicBoolean.class, new AtomicBoolean(false));
            cachedResultMember = codegenClassScope.makeAddMember(AtomicReference.class, new AtomicReference<Object>(null));
        }
        Class returnType = forge.getStaticMethod().getReturnType();

        CodegenMethodNode methodNode = codegenMethodScope.makeChild(forge.getEvaluationType(), ExprDotNodeForgeStaticMethodEval.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock();

        // check cached
        if (forge.isConstantParameters()) {
            CodegenBlock ifCached = block.ifCondition(exprDotMethod(member(isCachedMember.getMemberId()), "get"));
            if (returnType == void.class) {
                ifCached.blockReturnNoValue();
            } else {
                ifCached.blockReturn(cast(forge.getEvaluationType(), exprDotMethod(member(cachedResultMember.getMemberId()), "get")));
            }
        }

        // generate args
        CodegenExpression[] args = codegenArgExpressions(block, forge.getChildForges(), forge.getStaticMethod().getJavaMethod(), methodNode, exprSymbol, codegenClassScope);

        // try block
        CodegenBlock tryBlock = block.tryCatch();
        CodegenExpression invoke = codegenInvokeExpression(forge, args, codegenClassScope);
        if (returnType == void.class) {
            tryBlock.expression(invoke);
            if (forge.isConstantParameters()) {
                tryBlock.expression(exprDotMethod(member(isCachedMember.getMemberId()), "set", constantTrue()));
            }
            tryBlock.blockReturnNoValue();
        } else {
            tryBlock.declareVar(returnType, "result", invoke);

            if (forge.getChainForges().length == 0) {
                if (forge.isConstantParameters()) {
                    tryBlock.expression(exprDotMethod(member(cachedResultMember.getMemberId()), "set", ref("result")));
                    tryBlock.expression(exprDotMethod(member(isCachedMember.getMemberId()), "set", constantTrue()));
                }
                tryBlock.blockReturn(ref("result"));
            } else {
                tryBlock.declareVar(forge.getEvaluationType(), "chain", ExprDotNodeUtility.evaluateChainCodegen(methodNode, exprSymbol, codegenClassScope, ref("result"), returnType, forge.getChainForges(), forge.getResultWrapLambda()));
                if (forge.isConstantParameters()) {
                    tryBlock.expression(exprDotMethod(member(cachedResultMember.getMemberId()), "set", ref("chain")));
                    tryBlock.expression(exprDotMethod(member(isCachedMember.getMemberId()), "set", constantTrue()));
                }
                tryBlock.blockReturn(ref("chain"));
            }
        }

        // exception handling
        CodegenBlock catchBlock = tryBlock.tryEnd().addCatch(Throwable.class, "t")
                .declareVar(Object[].class, "argArray", newArrayByLength(Object.class, constant(args.length)));
        for (int i = 0; i < args.length; i++) {
            catchBlock.assignArrayElement("argArray", constant(i), args[i]);
        }
        catchBlock.staticMethod(ExprDotNodeForgeStaticMethodEval.class, METHOD_STATICMETHODEVALHANDLEINVOCATIONEXCEPTION,
                constant(forge.getStatementName()), member(methodMember.getMemberId()), constant(forge.getClassOrPropertyName()), ref("argArray"), ref("t"), constant(forge.isRethrowExceptions()));

        // end method
        if (returnType == void.class) {
            block.methodEnd();
        } else {
            block.methodReturn(constantNull());
        }
        return localMethod(methodNode);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object[] args = new Object[childEvals.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = childEvals[i].evaluate(new EventBean[]{eventBean}, false, null);
        }

        // The method is static so the object it is invoked on
        // can be null
        try {
            return forge.getStaticMethod().invoke(forge.getTargetObject(), args);
        } catch (InvocationTargetException e) {
            String message = JavaClassHelper.getMessageInvocationTarget(forge.getStatementName(), forge.getStaticMethod().getJavaMethod(), forge.getClassOrPropertyName(), args, e.getTargetException());
            log.error(message, e.getTargetException());
            if (forge.isRethrowExceptions()) {
                throw new EPException(message, e.getTargetException());
            }
        }
        return null;
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return false;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     *
     * @param statementName       stmt name
     * @param method              method
     * @param classOrPropertyName target name
     * @param args                args
     * @param targetException     exception
     * @param rethrow             indicator whether to rethrow
     */
    public static void staticMethodEvalHandleInvocationException(String statementName,
                                                                 Method method,
                                                                 String classOrPropertyName,
                                                                 Object[] args,
                                                                 Throwable targetException,
                                                                 boolean rethrow) {
        String message = JavaClassHelper.getMessageInvocationTarget(statementName, method, classOrPropertyName, args, targetException);
        log.error(message, targetException);
        if (rethrow) {
            throw new EPException(message, targetException);
        }
    }

    private static CodegenExpression codegenInvokeExpression(ExprDotNodeForgeStaticMethod forge, CodegenExpression[] args, CodegenClassScope codegenClassScope) {
        if (forge.getTargetObject() == null) {
            return staticMethod(forge.getStaticMethod().getDeclaringClass(), forge.getStaticMethod().getJavaMethod().getName(), args);
        } else {
            if (forge.getTargetObject().getClass().isEnum()) {
                return exprDotMethod(enumValue(forge.getTargetObject().getClass(), forge.getTargetObject().toString()), forge.getStaticMethod().getName(), args);
            } else {
                CodegenMember target = codegenClassScope.makeAddMember(forge.getTargetObject().getClass(), forge.getTargetObject());
                return exprDotMethod(member(target.getMemberId()), forge.getStaticMethod().getJavaMethod().getName(), args);
            }
        }
    }

    private static CodegenExpression[] codegenArgExpressions(CodegenBlock block, ExprForge[] forges, Method method, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression[] args = new CodegenExpression[forges.length];
        for (int i = 0; i < forges.length; i++) {
            String name = "r" + i;
            args[i] = ref(name);
            ExprForge child = forges[i];
            Class childType = child.getEvaluationType();
            if (childType == null) {
                block.declareVar(method.getParameterTypes()[i], name, constantNull());
            } else {
                block.declareVar(childType, name, child.evaluateCodegen(childType, codegenMethodScope, exprSymbol, codegenClassScope));
            }
        }
        return args;
    }
}
