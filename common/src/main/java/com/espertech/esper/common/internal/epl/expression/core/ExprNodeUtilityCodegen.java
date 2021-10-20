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
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeNull;
import com.espertech.esper.common.client.type.EPTypePremade;
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

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;

public class ExprNodeUtilityCodegen {
    public static CodegenExpression codegenExpressionMayCoerce(ExprForge forge, EPType targetType, CodegenMethod exprMethod, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope classScope) {
        if (targetType == EPTypeNull.INSTANCE) {
            return constantNull();
        }
        CodegenExpression expr = forge.evaluateCodegen((EPTypeClass) forge.getEvaluationType(), exprMethod, exprSymbol, classScope);
        return ExprNodeUtilityCodegen.codegenCoerce(expr, forge.getEvaluationType(), targetType, false);
    }

    public static CodegenExpression codegenCoerce(CodegenExpression expression, EPType exprType, EPType targetType, boolean alwaysCast) {
        if (targetType == null || targetType == EPTypeNull.INSTANCE || exprType == null || exprType == EPTypeNull.INSTANCE) {
            return expression;
        }
        EPTypeClass exprClass = (EPTypeClass) exprType;
        EPTypeClass exprClassBoxed = JavaClassHelper.getBoxedType(exprClass);
        EPTypeClass targetClass = (EPTypeClass) targetType;
        EPTypeClass targetClassBoxed = JavaClassHelper.getBoxedType(targetClass);
        if (exprClassBoxed.getType() == targetClassBoxed.getType()) {
            return alwaysCast ? cast(targetClass, expression) : expression;
        }
        SimpleNumberCoercer coercer = SimpleNumberCoercerFactory.getCoercer(exprClass, JavaClassHelper.getBoxedType(targetClass));
        if (exprClass.getType().isPrimitive() || alwaysCast) {
            expression = cast(exprClassBoxed, expression);
        }
        return coercer.coerceCodegen(expression, exprClass);
    }

    public static CodegenExpressionNewAnonymousClass codegenEvaluator(ExprForge forge, CodegenMethod method, Class originator, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass anonymousClass = newAnonymousClass(method.getBlock(), ExprEvaluator.EPTYPE);
        CodegenMethod evaluate = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), originator, classScope).addParam(ExprForgeCodegenNames.PARAMS);
        anonymousClass.addMethod("evaluate", evaluate);
        EPType type = forge.getEvaluationType();

        if (type == null || type == EPTypeNull.INSTANCE) {
            evaluate.getBlock().methodReturn(constantNull());
            return anonymousClass;
        }
        EPTypeClass typeClass = (EPTypeClass) type;
        if (JavaClassHelper.isTypeVoid(typeClass)) {
            CodegenMethod evalMethod = CodegenLegoMethodExpression.codegenExpression(forge, method, classScope);
            evaluate.getBlock()
                    .localMethod(evalMethod, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT)
                    .methodReturn(constantNull());
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
        return newArrayWithInit(ExprEvaluator.EPTYPEARRAY, init);
    }

    public static CodegenExpression codegenEvaluators(ExprForge[] expressions, CodegenMethodScope parent, Class originator, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(ExprEvaluator.EPTYPEARRAY, originator, classScope);
        method.getBlock().declareVar(ExprEvaluator.EPTYPEARRAY, "evals", newArrayByLength(ExprEvaluator.EPTYPE, constant(expressions.length)));
        for (int i = 0; i < expressions.length; i++) {
            method.getBlock().assignArrayElement("evals", constant(i), expressions[i] == null ? constantNull() : codegenEvaluator(expressions[i], method, originator, classScope));
        }
        method.getBlock().methodReturn(ref("evals"));
        return localMethod(method);
    }

    public static CodegenExpressionNewAnonymousClass codegenEvaluatorNoCoerce(ExprForge forge, CodegenMethod method, Class generator, CodegenClassScope classScope) {
        return codegenEvaluatorWCoerce(forge, null, method, generator, classScope);
    }

    public static CodegenExpressionNewAnonymousClass codegenEvaluatorWCoerce(ExprForge forge, EPTypeClass optCoercionType, CodegenMethod method, Class generator, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass evaluator = newAnonymousClass(method.getBlock(), ExprEvaluator.EPTYPE);
        CodegenMethod evaluate = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), generator, classScope).addParam(ExprForgeCodegenNames.PARAMS);
        evaluator.addMethod("evaluate", evaluate);

        CodegenExpression result = constantNull();
        if (forge.getEvaluationType() != null) {
            CodegenMethod evalMethod = CodegenLegoMethodExpression.codegenExpression(forge, method, classScope);
            result = localMethod(evalMethod, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT);

            if (optCoercionType != null && !EPTypeNull.INSTANCE.equals(forge.getEvaluationType())) {
                EPTypeClass type = (EPTypeClass) forge.getEvaluationType();
                EPTypeClass boxed = JavaClassHelper.getBoxedType(type);
                if (boxed.getType() != JavaClassHelper.getBoxedType(optCoercionType).getType()) {
                    SimpleNumberCoercer coercer = SimpleNumberCoercerFactory.getCoercer(boxed, JavaClassHelper.getBoxedType(optCoercionType));
                    evaluate.getBlock().declareVar(boxed, "result", result);
                    result = coercer.coerceCodegen(ref("result"), boxed);
                }
            }
        }

        evaluate.getBlock().methodReturn(result);
        return evaluator;
    }

    public static CodegenExpressionNewAnonymousClass codegenEvaluatorObjectArray(ExprForge[] forges, CodegenMethod method, Class generator, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass evaluator = newAnonymousClass(method.getBlock(), ExprEvaluator.EPTYPE);
        CodegenMethod evaluate = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), generator, classScope).addParam(ExprForgeCodegenNames.PARAMS);
        evaluator.addMethod("evaluate", evaluate);

        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
        CodegenMethod exprMethod = evaluate.makeChildWithScope(EPTypePremade.OBJECT.getEPType(), CodegenLegoMethodExpression.class, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);

        CodegenExpression[] expressions = new CodegenExpression[forges.length];
        for (int i = 0; i < forges.length; i++) {
            EPType type = forges[i].getEvaluationType();
            if (type == null || type == EPTypeNull.INSTANCE) {
                expressions[i] = constantNull();
            } else {
                expressions[i] = forges[i].evaluateCodegen((EPTypeClass) type, exprMethod, exprSymbol, classScope);
            }
        }
        exprSymbol.derivedSymbolsCodegen(evaluate, exprMethod.getBlock(), classScope);

        exprMethod.getBlock().declareVar(EPTypePremade.OBJECTARRAY.getEPType(), "values", newArrayByLength(EPTypePremade.OBJECT.getEPType(), constant(forges.length)));
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
        CodegenMethod method = parent.makeChildWithScope(EPTypePremade.MAP.getEPType(), generator, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);

        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "map", newInstance(EPTypePremade.HASHMAP.getEPType(), constant(selectAsNames.length + 2)));
        CodegenExpression[] expressions = new CodegenExpression[selectAsNames.length];
        for (int i = 0; i < selectClause.length; i++) {
            expressions[i] = selectClause[i].getForge().evaluateCodegen(EPTypePremade.OBJECT.getEPType(), method, exprSymbol, classScope);
        }

        exprSymbol.derivedSymbolsCodegen(method, method.getBlock(), classScope);

        for (int i = 0; i < selectClause.length; i++) {
            method.getBlock().exprDotMethod(ref("map"), "put", constant(selectAsNames[i]), expressions[i]);
        }

        method.getBlock().methodReturn(ref("map"));
        return method;
    }

    public static CodegenExpression codegenExprEnumEval(ExprEnumerationGivenEventForge enumEval, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope, Class generator) {
        CodegenExpressionNewAnonymousClass evaluator = newAnonymousClass(method.getBlock(), ExprEnumerationGivenEvent.EPTYPE);

        ExprEnumerationGivenEventSymbol enumSymbols = new ExprEnumerationGivenEventSymbol();
        CodegenMethod evaluateEventGetROCollectionEvents = CodegenMethod.makeParentNode(EPTypePremade.COLLECTION.getEPType(), generator, enumSymbols, classScope).addParam(EventBean.EPTYPE, "event").addParam(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
        evaluator.addMethod("evaluateEventGetROCollectionEvents", evaluateEventGetROCollectionEvents);
        evaluateEventGetROCollectionEvents.getBlock().methodReturn(enumEval.evaluateEventGetROCollectionEventsCodegen(evaluateEventGetROCollectionEvents, enumSymbols, classScope));

        CodegenMethod evaluateEventGetROCollectionScalar = CodegenMethod.makeParentNode(EPTypePremade.COLLECTION.getEPType(), generator, enumSymbols, classScope).addParam(EventBean.EPTYPE, "event").addParam(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
        evaluator.addMethod("evaluateEventGetROCollectionScalar", evaluateEventGetROCollectionScalar);
        evaluateEventGetROCollectionScalar.getBlock().methodReturn(enumEval.evaluateEventGetROCollectionScalarCodegen(evaluateEventGetROCollectionScalar, enumSymbols, classScope));

        CodegenMethod evaluateEventGetEventBean = CodegenMethod.makeParentNode(EventBean.EPTYPE, generator, enumSymbols, classScope).addParam(EventBean.EPTYPE, "event").addParam(ExprEvaluatorContext.EPTYPE, NAME_EXPREVALCONTEXT);
        evaluator.addMethod("evaluateEventGetEventBean", evaluateEventGetEventBean);
        evaluateEventGetEventBean.getBlock().methodReturn(enumEval.evaluateEventGetEventBeanCodegen(evaluateEventGetEventBean, enumSymbols, classScope));

        return evaluator;
    }
}
