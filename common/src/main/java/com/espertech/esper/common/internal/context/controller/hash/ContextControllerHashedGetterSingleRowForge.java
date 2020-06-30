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
package com.espertech.esper.common.internal.context.controller.hash;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRelational;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.codegen.StaticMethodCodegenArgDesc;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.event.core.EventPropertyValueGetterForge;
import com.espertech.esper.common.internal.settings.ClasspathImportSingleRowDesc;
import com.espertech.esper.common.internal.util.ClassHelperGenericType;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.lang.reflect.Method;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.StaticMethodCallHelper.*;

public class ContextControllerHashedGetterSingleRowForge implements EventPropertyValueGetterForge {
    private final Method reflectionMethod;
    private final ExprForge[] nodes;
    private final int granularity;
    private final String statementName;

    public ContextControllerHashedGetterSingleRowForge(Pair<Class, ClasspathImportSingleRowDesc> func, List<ExprNode> parameters, int granularity, EventType eventType, StatementRawInfo statementRawInfo, StatementCompileTimeServices services)
            throws ExprValidationException {
        ExprNodeUtilMethodDesc staticMethodDesc = ExprNodeUtilityResolve.resolveMethodAllowWildcardAndStream(func.getFirst().getName(), null, func.getSecond().getMethodName(),
                parameters, true, eventType, new ExprNodeUtilResolveExceptionHandlerDefault(func.getSecond().getMethodName(), true), func.getSecond().getMethodName(), statementRawInfo, services);
        this.granularity = granularity;
        this.nodes = staticMethodDesc.getChildForges();
        this.reflectionMethod = staticMethodDesc.getReflectionMethod();
        this.statementName = statementRawInfo.getStatementName();
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(EPTypePremade.OBJECT.getEPType(), this.getClass(), classScope).addParam(EventBean.EPTYPE, "eventBean");
        method.getBlock().declareVar(EventBean.EPTYPEARRAY, "events", newArrayWithInit(EventBean.EPTYPE, ref("eventBean")));

        // method to evaluate expressions and compute hash
        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, true);
        EPTypeClass returnType = ClassHelperGenericType.getMethodReturnEPType(reflectionMethod);
        CodegenMethod exprMethod = method.makeChildWithScope(returnType, CodegenLegoMethodExpression.class, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);

        // generate args
        StaticMethodCodegenArgDesc[] args = allArgumentExpressions(nodes, reflectionMethod, exprMethod, exprSymbol, classScope);
        appendArgExpressions(args, exprMethod.getBlock());

        // try block
        CodegenBlock tryBlock = exprMethod.getBlock().tryCatch();
        CodegenExpression invoke = codegenInvokeExpression(null, reflectionMethod, args, classScope);
        tryBlock.blockReturn(invoke);

        // exception handling
        appendCatch(tryBlock, reflectionMethod, statementName, reflectionMethod.getDeclaringClass().getName(), true, args);

        exprMethod.getBlock().methodReturn(constant(0));

        EPTypeClass returnTypeMethod = ClassHelperGenericType.getMethodReturnEPType(reflectionMethod);
        method.getBlock().declareVar(returnTypeMethod, "result", localMethod(exprMethod, ref("events"), constantTrue(), constantNull()));
        if (!reflectionMethod.getReturnType().isPrimitive()) {
            method.getBlock().ifRefNull("result").blockReturn(constant(0));
        }
        method.getBlock().declareVar(EPTypePremade.INTEGERPRIMITIVE.getEPType(), "value", SimpleNumberCoercerFactory.getCoercer(returnTypeMethod, EPTypePremade.INTEGERBOXED.getEPType()).coerceCodegen(ref("result"), returnTypeMethod))
                .ifCondition(relational(ref("value"), CodegenExpressionRelational.CodegenRelational.GE, constant(0)))
                .blockReturn(op(ref("value"), "%", constant(granularity)))
                .methodReturn(op(op(ref("value"), "%", constant(granularity)), "*", constant(-1)));
        return localMethod(method, beanExpression);
    }
}
