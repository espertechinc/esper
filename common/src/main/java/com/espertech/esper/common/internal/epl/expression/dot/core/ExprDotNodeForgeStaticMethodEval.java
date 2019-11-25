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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.codegen.StaticMethodCodegenArgDesc;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.metrics.instrumentation.InstrumentationCode;
import com.espertech.esper.common.internal.rettype.ClassEPType;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeCodegenSharable;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.StaticMethodCallHelper.*;

public class ExprDotNodeForgeStaticMethodEval implements ExprEvaluator, EventPropertyGetter {
    private static final Logger log = LoggerFactory.getLogger(ExprDotNodeForgeStaticMethodEval.class);
    public final static String METHOD_STATICMETHODEVALHANDLEINVOCATIONEXCEPTION = "staticMethodEvalHandleInvocationException";

    private final ExprDotNodeForgeStaticMethod forge;
    private final ExprEvaluator[] childEvals;
    private final ExprDotEval[] chainEval;

    public ExprDotNodeForgeStaticMethodEval(ExprDotNodeForgeStaticMethod forge, ExprEvaluator[] childEvals, ExprDotEval[] chainEval) {
        this.forge = forge;
        this.childEvals = childEvals;
        this.chainEval = chainEval;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        Object[] args = new Object[childEvals.length];
        for (int i = 0; i < args.length; i++) {
            args[i] = childEvals[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
        }

        // The method is static so the object it is invoked on
        // can be null
        try {
            Object result = forge.getStaticMethod().invoke(forge.getTargetObject() == null ? null : forge.getTargetObject().getValue(), args);

            result = ExprDotNodeUtility.evaluateChainWithWrap(forge.getResultWrapLambda(), result, null, forge.getStaticMethod().getReturnType(), chainEval, forge.getChainForges(), eventsPerStream, isNewData, exprEvaluatorContext);

            return result;
        } catch (InvocationTargetException | IllegalAccessException e) {
            staticMethodEvalHandleInvocationException(null, forge.getStaticMethod().getName(), forge.getStaticMethod().getParameterTypes(), forge.getClassOrPropertyName(), args, e, forge.isRethrowExceptions());
        }
        return null;
    }

    public static CodegenExpression codegenExprEval(ExprDotNodeForgeStaticMethod forge, CodegenMethodScope codegenMethodScope, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenExpression isCachedMember = null;
        CodegenExpression cachedResultMember = null;
        if (forge.isConstantParameters()) {
            isCachedMember = codegenClassScope.addFieldUnshared(false, boolean.class, constantFalse());
            cachedResultMember = codegenClassScope.addFieldUnshared(false, Object.class, constantNull());
        }
        Class returnType = forge.getStaticMethod().getReturnType();

        CodegenMethod methodNode = codegenMethodScope.makeChild(forge.getEvaluationType(), ExprDotNodeForgeStaticMethodEval.class, codegenClassScope);

        CodegenBlock block = methodNode.getBlock();

        // check cached
        if (forge.isConstantParameters()) {
            CodegenBlock ifCached = block.ifCondition(isCachedMember);
            if (returnType == void.class) {
                ifCached.blockReturnNoValue();
            } else {
                ifCached.blockReturn(cast(forge.getEvaluationType(), cachedResultMember));
            }
        }

        // generate args
        StaticMethodCodegenArgDesc[] args = allArgumentExpressions(forge.getChildForges(), forge.getStaticMethod(), methodNode, exprSymbol, codegenClassScope);
        appendArgExpressions(args, methodNode.getBlock());

        // try block
        CodegenBlock tryBlock = block.tryCatch();
        CodegenExpression invoke = codegenInvokeExpression(forge.getTargetObject(), forge.getStaticMethod(), args, codegenClassScope);
        if (returnType == void.class) {
            tryBlock.expression(invoke);
            if (forge.isConstantParameters()) {
                tryBlock.assignRef(isCachedMember, constantTrue());
            }
            tryBlock.blockReturnNoValue();
        } else {
            tryBlock.declareVar(returnType, "result", invoke);

            if (forge.getChainForges().length == 0) {
                CodegenExpression typeInformation = constantNull();
                if (codegenClassScope.isInstrumented()) {
                    typeInformation = codegenClassScope.addOrGetFieldSharable(new EPTypeCodegenSharable(new ClassEPType(forge.getEvaluationType()), codegenClassScope));
                }

                tryBlock.apply(InstrumentationCode.instblock(codegenClassScope, "qExprDotChain", typeInformation, ref("result"), constant(0)));
                if (forge.isConstantParameters()) {
                    tryBlock.assignRef(cachedResultMember, ref("result"));
                    tryBlock.assignRef(isCachedMember, constantTrue());
                }
                tryBlock.apply(InstrumentationCode.instblock(codegenClassScope, "aExprDotChain"))
                    .blockReturn(ref("result"));
            } else {
                EPType typeInfo;
                if (forge.getResultWrapLambda() != null) {
                    typeInfo = forge.getResultWrapLambda().getTypeInfo();
                } else {
                    typeInfo = new ClassEPType(Object.class);
                }

                CodegenExpression typeInformation = constantNull();
                if (codegenClassScope.isInstrumented()) {
                    typeInformation = codegenClassScope.addOrGetFieldSharable(new EPTypeCodegenSharable(typeInfo, codegenClassScope));
                }

                tryBlock.apply(InstrumentationCode.instblock(codegenClassScope, "qExprDotChain", typeInformation, ref("result"), constant(forge.getChainForges().length)))
                    .declareVar(forge.getEvaluationType(), "chain", ExprDotNodeUtility.evaluateChainCodegen(methodNode, exprSymbol, codegenClassScope, ref("result"), returnType, forge.getChainForges(), forge.getResultWrapLambda()));
                if (forge.isConstantParameters()) {
                    tryBlock.assignRef(cachedResultMember, ref("chain"));
                    tryBlock.assignRef(isCachedMember, constantTrue());
                }
                tryBlock.apply(InstrumentationCode.instblock(codegenClassScope, "aExprDotChain"))
                    .blockReturn(ref("chain"));
            }
        }

        // exception handling
        appendCatch(tryBlock, forge.getStaticMethod(), forge.getOptionalStatementName(), forge.getClassOrPropertyName(), forge.isRethrowExceptions(), args);

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
            args[i] = childEvals[i].evaluate(new EventBean[]{eventBean}, true, null);
        }

        // The method is static so the object it is invoked on
        // can be null
        try {
            return forge.getStaticMethod().invoke(forge.getTargetObject() == null ? null : forge.getTargetObject().getValue(), args);
        } catch (InvocationTargetException | IllegalAccessException e) {
            staticMethodEvalHandleInvocationException(forge.getOptionalStatementName(), forge.getStaticMethod().getName(), forge.getStaticMethod().getParameterTypes(), forge.getClassOrPropertyName(), args, e, forge.isRethrowExceptions());
        }
        return null;
    }

    public static CodegenExpression codegenGet(CodegenExpression beanExpression, ExprDotNodeForgeStaticMethod forge, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
        CodegenMethod methodNode = codegenMethodScope.makeChildWithScope(forge.getEvaluationType(), ExprDotNodeForgeStaticMethodEval.class, exprSymbol, codegenClassScope).addParam(ExprForgeCodegenNames.PARAMS);

        StaticMethodCodegenArgDesc[] args = allArgumentExpressions(forge.getChildForges(), forge.getStaticMethod(), methodNode, exprSymbol, codegenClassScope);
        exprSymbol.derivedSymbolsCodegen(methodNode, methodNode.getBlock(), codegenClassScope);
        appendArgExpressions(args, methodNode.getBlock());

        // try block
        CodegenBlock tryBlock = methodNode.getBlock().tryCatch();
        CodegenExpression invoke = codegenInvokeExpression(forge.getTargetObject(), forge.getStaticMethod(), args, codegenClassScope);
        tryBlock.blockReturn(invoke);

        // exception handling
        appendCatch(tryBlock, forge.getStaticMethod(), forge.getOptionalStatementName(), forge.getClassOrPropertyName(), forge.isRethrowExceptions(), args);

        // end method
        methodNode.getBlock().methodReturn(constantNull());

        return localMethod(methodNode, newArrayWithInit(EventBean.class, beanExpression), constantTrue(), constantNull());
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
     * @param optionalStatementName stmt name
     * @param methodName            methodName
     * @param parameterTypes        param types
     * @param classOrPropertyName   target name
     * @param args                  args
     * @param thrown                exception
     * @param rethrow               indicator whether to rethrow
     */
    public static void staticMethodEvalHandleInvocationException(String optionalStatementName,
                                                                 String methodName,
                                                                 Class[] parameterTypes,
                                                                 String classOrPropertyName,
                                                                 Object[] args,
                                                                 Throwable thrown,
                                                                 boolean rethrow) {
        Throwable indication = thrown instanceof InvocationTargetException ? ((InvocationTargetException) thrown).getTargetException() : thrown;
        String message = JavaClassHelper.getMessageInvocationTarget(optionalStatementName, methodName, parameterTypes, classOrPropertyName, args, indication);
        log.error(message, indication);
        if (rethrow) {
            throw new EPException(message, indication);
        }
    }
}
