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
package com.espertech.esper.common.internal.epl.expression.core;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import com.espertech.esper.common.internal.util.SimpleNumberCoercer;
import com.espertech.esper.common.internal.util.SimpleNumberCoercerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;

public class ExprNodeUtilityCodegen {
    public static CodegenExpression codegenExpressionMayCoerce(ExprForge forge, Class targetType, CodegenMethod exprMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope classScope) {
        CodegenExpression expr = forge.evaluateCodegen(forge.getEvaluationType(), exprMethod, exprSymbol, classScope);
        return ExprNodeUtilityCodegen.codegenCoerce(expr, forge.getEvaluationType(), targetType, false);
    }

    public static CodegenExpression codegenCoerce(CodegenExpression expression, Class exprType, Class targetType, boolean alwaysCast) {
        if (targetType == null) {
            return expression;
        }
        if (JavaClassHelper.getBoxedType(exprType) == JavaClassHelper.getBoxedType(targetType)) {
            return alwaysCast ? cast(targetType, expression) : expression;
        }
        SimpleNumberCoercer coercer = SimpleNumberCoercerFactory.getCoercer(exprType, JavaClassHelper.getBoxedType(targetType));
        if (exprType.isPrimitive() || alwaysCast) {
            expression = cast(JavaClassHelper.getBoxedType(exprType), expression);
        }
        return coercer.coerceCodegen(expression, exprType);
    }

    public static CodegenExpressionNewAnonymousClass codegenEvaluator(ExprForge forge, CodegenMethod method, Class originator, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass anonymousClass = newAnonymousClass(method.getBlock(), ExprEvaluator.class);
        CodegenMethod evaluate = CodegenMethod.makeParentNode(Object.class, originator, classScope).addParam(ExprForgeCodegenNames.PARAMS);
        anonymousClass.addMethod("evaluate", evaluate);
        if (forge.getEvaluationType() == null) {
            evaluate.getBlock().methodReturn(constantNull());
        } else {
            CodegenMethod evalMethod = CodegenLegoMethodExpression.codegenExpression(forge, method, classScope);
            evaluate.getBlock().methodReturn(localMethod(evalMethod, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
        }
        return anonymousClass;
    }

    public static CodegenExpression codegenEvaluators(ExprNode[] expressions, CodegenMethodScope parent, Class originator, CodegenClassScope classScope) {
        return codegenEvaluators(ExprNodeUtilityQuery.getForges(expressions), parent, originator, classScope);
    }

    public static CodegenExpression codegenEvaluators(ExprForge[][] expressions, CodegenMethodScope parent, Class originator, CodegenClassScope classScope) {
        CodegenExpression[] init = new CodegenExpression[expressions.length];
        for (int i = 0; i < init.length; i++) {
            init[i] = codegenEvaluators(expressions[i], parent, originator, classScope);
        }
        return newArrayWithInit(ExprEvaluator[].class, init);
    }

    public static CodegenExpression codegenEvaluators(ExprForge[] expressions, CodegenMethodScope parent, Class originator, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ExprEvaluator[].class, originator, classScope);
        method.getBlock().declareVar(ExprEvaluator[].class, "evals", newArrayByLength(ExprEvaluator.class, constant(expressions.length)));
        for (int i = 0; i < expressions.length; i++) {
            method.getBlock().assignArrayElement("evals", constant(i), expressions[i] == null ? constantNull() : codegenEvaluator(expressions[i], method, originator, classScope));
        }
        method.getBlock().methodReturn(ref("evals"));
        return localMethod(method);
    }

    public static CodegenExpressionNewAnonymousClass codegenEvaluatorNoCoerce(ExprForge forge, CodegenMethod method, Class generator, CodegenClassScope classScope) {
        return codegenEvaluatorWCoerce(forge, null, method, generator, classScope);
    }

    public static CodegenExpressionNewAnonymousClass codegenEvaluatorWCoerce(ExprForge forge, Class optCoercionType, CodegenMethod method, Class generator, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass evaluator = newAnonymousClass(method.getBlock(), ExprEvaluator.class);
        CodegenMethod evaluate = CodegenMethod.makeParentNode(Object.class, generator, classScope).addParam(ExprForgeCodegenNames.PARAMS);
        evaluator.addMethod("evaluate", evaluate);

        CodegenExpression result = constantNull();
        if (forge.getEvaluationType() != null) {
            CodegenMethod evalMethod = CodegenLegoMethodExpression.codegenExpression(forge, method, classScope);
            result = localMethod(evalMethod, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT);

            if (optCoercionType != null && JavaClassHelper.getBoxedType(forge.getEvaluationType()) != JavaClassHelper.getBoxedType(optCoercionType)) {
                SimpleNumberCoercer coercer = SimpleNumberCoercerFactory.getCoercer(forge.getEvaluationType(), JavaClassHelper.getBoxedType(optCoercionType));
                evaluate.getBlock().declareVar(forge.getEvaluationType(), "result", result);
                result = coercer.coerceCodegen(ref("result"), forge.getEvaluationType());
            }
        }

        evaluate.getBlock().methodReturn(result);
        return evaluator;
    }

    public static CodegenExpressionNewAnonymousClass codegenEvaluatorObjectArray(ExprForge[] forges, CodegenMethod method, Class generator, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass evaluator = newAnonymousClass(method.getBlock(), ExprEvaluator.class);
        CodegenMethod evaluate = CodegenMethod.makeParentNode(Object.class, generator, classScope).addParam(ExprForgeCodegenNames.PARAMS);
        evaluator.addMethod("evaluate", evaluate);

        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
        CodegenMethod exprMethod = evaluate.makeChildWithScope(Object.class, CodegenLegoMethodExpression.class, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);

        CodegenExpression[] expressions = new CodegenExpression[forges.length];
        for (int i = 0; i < forges.length; i++) {
            expressions[i] = forges[i].evaluateCodegen(forges[i].getEvaluationType(), exprMethod, exprSymbol, classScope);
        }
        exprSymbol.derivedSymbolsCodegen(evaluate, exprMethod.getBlock(), classScope);

        exprMethod.getBlock().declareVar(Object[].class, "values", newArrayByLength(Object.class, constant(forges.length)));
        for (int i = 0; i < forges.length; i++) {
            CodegenExpression result = expressions[i];
            exprMethod.getBlock().assignArrayElement("values", constant(i), result);
        }
        exprMethod.getBlock().methodReturn(ref("values"));
        evaluate.getBlock().methodReturn(localMethod(exprMethod, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));

        return evaluator;
    }

    public static CodegenMethod codegenMapSelect(ExprNode[] selectClause, String[] selectAsNames, Class generator, CodegenMethodScope parent, CodegenClassScope classScope) {
        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
        CodegenMethod method = parent.makeChildWithScope(Map.class, generator, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);

        method.getBlock().declareVar(Map.class, "map", newInstance(HashMap.class, constant(selectAsNames.length + 2)));
        CodegenExpression[] expressions = new CodegenExpression[selectAsNames.length];
        for (int i = 0; i < selectClause.length; i++) {
            expressions[i] = selectClause[i].getForge().evaluateCodegen(Object.class, method, exprSymbol, classScope);
        }

        exprSymbol.derivedSymbolsCodegen(method, method.getBlock(), classScope);

        for (int i = 0; i < selectClause.length; i++) {
            method.getBlock().exprDotMethod(ref("map"), "put", constant(selectAsNames[i]), expressions[i]);
        }

        method.getBlock().methodReturn(ref("map"));
        return method;
    }

    public static CodegenExpression codegenExprEnumEval(ExprEnumerationGivenEventForge enumEval, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope, Class generator) {
        CodegenExpressionNewAnonymousClass evaluator = newAnonymousClass(method.getBlock(), ExprEnumerationGivenEvent.class);

        ExprEnumerationGivenEventSymbol enumSymbols = new ExprEnumerationGivenEventSymbol();
        CodegenMethod evaluateEventGetROCollectionEvents = CodegenMethod.makeParentNode(Collection.class, generator, enumSymbols, classScope).addParam(EventBean.class, "event").addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        evaluator.addMethod("evaluateEventGetROCollectionEvents", evaluateEventGetROCollectionEvents);
        evaluateEventGetROCollectionEvents.getBlock().methodReturn(enumEval.evaluateEventGetROCollectionEventsCodegen(evaluateEventGetROCollectionEvents, enumSymbols, classScope));

        CodegenMethod evaluateEventGetROCollectionScalar = CodegenMethod.makeParentNode(Collection.class, generator, enumSymbols, classScope).addParam(EventBean.class, "event").addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        evaluator.addMethod("evaluateEventGetROCollectionScalar", evaluateEventGetROCollectionScalar);
        evaluateEventGetROCollectionScalar.getBlock().methodReturn(enumEval.evaluateEventGetROCollectionScalarCodegen(evaluateEventGetROCollectionScalar, enumSymbols, classScope));

        CodegenMethod evaluateEventGetEventBean = CodegenMethod.makeParentNode(EventBean.class, generator, enumSymbols, classScope).addParam(EventBean.class, "event").addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        evaluator.addMethod("evaluateEventGetEventBean", evaluateEventGetEventBean);
        evaluateEventGetEventBean.getBlock().methodReturn(enumEval.evaluateEventGetEventBeanCodegen(evaluateEventGetEventBean, enumSymbols, classScope));

        return evaluator;
    }
}
