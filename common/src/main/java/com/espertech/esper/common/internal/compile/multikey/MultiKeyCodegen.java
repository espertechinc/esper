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
package com.espertech.esper.common.internal.compile.multikey;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.MultiKey;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSymbolProviderEmpty;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.collection.MultiKeyFromMultiKey;
import com.espertech.esper.common.internal.collection.MultiKeyFromObjectArray;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.*;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.event.variant.VariantEventType;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;
import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen.codegenExpressionMayCoerce;

public class MultiKeyCodegen {

    public static CodegenExpressionNewAnonymousClass codegenEvaluatorReturnObjectOrArray(ExprForge[] forges, CodegenMethod method, Class generator, CodegenClassScope classScope) {
        return codegenEvaluatorReturnObjectOrArrayWCoerce(forges, null, false, method, generator, classScope);
    }

    public static CodegenExpressionNewAnonymousClass codegenEvaluatorReturnObjectOrArrayWCoerce(ExprForge[] forges, Class[] targetTypes, boolean arrayMultikeyWhenSingleEvaluator, CodegenMethod method, Class generator, CodegenClassScope classScope) {
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

        if (forges.length == 0) {
            exprMethod.getBlock().methodReturn(constantNull());
        } else if (forges.length == 1) {
            Class evaluationType = forges[0].getEvaluationType();
            CodegenExpression coerced;
            if (arrayMultikeyWhenSingleEvaluator && evaluationType.isArray()) {
                Class clazz = MultiKeyPlanner.getMKClassForComponentType(evaluationType.getComponentType());
                coerced = newInstance(clazz, expressions[0]);
            } else {
                coerced = ExprNodeUtilityCodegen.codegenCoerce(expressions[0], evaluationType, targetTypes == null ? null : targetTypes[0], false);
            }
            exprMethod.getBlock().methodReturn(coerced);
        } else {
            exprMethod.getBlock().declareVar(Object[].class, "values", newArrayByLength(Object.class, constant(forges.length)));
            for (int i = 0; i < forges.length; i++) {
                CodegenExpression coerced = ExprNodeUtilityCodegen.codegenCoerce(expressions[i], forges[i].getEvaluationType(), targetTypes == null ? null : targetTypes[i], false);
                exprMethod.getBlock().assignArrayElement("values", constant(i), coerced);
            }
            exprMethod.getBlock().methodReturn(ref("values"));
        }
        evaluate.getBlock().methodReturn(localMethod(exprMethod, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
        return evaluator;
    }

    public static CodegenExpression codegenExprEvaluatorMayMultikey(ExprNode[] expressionNodes, Class[] optionalCoercionTypes, MultiKeyClassRef multiKeyClassRef, CodegenMethod method, CodegenClassScope classScope) {
        if (expressionNodes == null || expressionNodes.length == 0) {
            return constantNull();
        }
        return codegenExprEvaluatorMayMultikey(ExprNodeUtilityQuery.getForges(expressionNodes), optionalCoercionTypes, multiKeyClassRef, method, classScope);
    }

    public static CodegenExpression codegenExprEvaluatorMayMultikey(ExprForge[] forges, Class[] optionalCoercionTypes, MultiKeyClassRef multiKeyClassRef, CodegenMethod method, CodegenClassScope classScope) {
        if (forges == null || forges.length == 0) {
            return constantNull();
        }
        if (multiKeyClassRef != null && multiKeyClassRef.getClassNameMK() != null) {
            return codegenMultiKeyExprEvaluator(forges, multiKeyClassRef, method, classScope);
        }
        return ExprNodeUtilityCodegen.codegenEvaluatorWCoerce(forges[0], optionalCoercionTypes == null ? null : optionalCoercionTypes[0],
            method, MultiKeyCodegen.class, classScope);
    }

    public static CodegenMethod codegenMethod(ExprNode[] expressionNodes, MultiKeyClassRef multiKeyClassRef, CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod eventUnpackMethod = parent.makeChildWithScope(Object.class, CodegenLegoMethodExpression.class, CodegenSymbolProviderEmpty.INSTANCE, classScope).addParam(ExprForgeCodegenNames.PARAMS);

        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
        CodegenMethod exprMethod = eventUnpackMethod.makeChildWithScope(Object.class, CodegenLegoMethodExpression.class, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);

        CodegenExpression[] expressions = new CodegenExpression[expressionNodes.length];
        for (int i = 0; i < expressionNodes.length; i++) {
            ExprForge forge = expressionNodes[i].getForge();
            expressions[i] = codegenExpressionMayCoerce(forge, multiKeyClassRef.getMKTypes()[i], exprMethod, exprSymbol, classScope);
        }

        exprSymbol.derivedSymbolsCodegen(eventUnpackMethod, exprMethod.getBlock(), classScope);
        exprMethod.getBlock().methodReturn(newInstance(multiKeyClassRef.getClassNameMK(), expressions));

        eventUnpackMethod.getBlock().methodReturn(localMethod(exprMethod, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
        return eventUnpackMethod;
    }

    public static CodegenExpression codegenGetterMayMultiKey(EventType eventType, EventPropertyGetterSPI[] getters, Class[] getterResultTypes, Class[] optionalCoercionTypes, MultiKeyClassRef multiKeyClassRef, CodegenMethod method, CodegenClassScope classScope) {
        if (getters == null || getters.length == 0) {
            return constantNull();
        }
        if (multiKeyClassRef != null && multiKeyClassRef.getClassNameMK() != null) {
            return codegenMultiKeyGetter(eventType, getters, getterResultTypes, multiKeyClassRef, method, classScope);
        }
        return EventTypeUtility.codegenGetterWCoerce(getters[0], getterResultTypes[0], optionalCoercionTypes == null ? null : optionalCoercionTypes[0], method, MultiKeyCodegen.class, classScope);
    }

    public static CodegenExpression codegenMultiKeyFromArrayTransform(MultiKeyClassRef optionalMultiKeyClasses, CodegenMethod method, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass fromClass = newAnonymousClass(method.getBlock(), MultiKeyFromObjectArray.class);
        CodegenMethod from = CodegenMethod.makeParentNode(Object.class, MultiKeyCodegen.class, classScope).addParam(Object[].class, "keys");
        fromClass.addMethod("from", from);

        if (optionalMultiKeyClasses == null || optionalMultiKeyClasses.getClassNameMK() == null) {
            from.getBlock().methodReturn(arrayAtIndex(ref("keys"), constant(0)));
        } else if (optionalMultiKeyClasses.getMKTypes().length == 1) {
            Class paramType = optionalMultiKeyClasses.getMKTypes()[0];
            if (paramType == null || !paramType.isArray()) {
                from.getBlock().methodReturn(arrayAtIndex(ref("keys"), constant(0)));
            } else {
                Class mktype = MultiKeyPlanner.getMKClassForComponentType(paramType.getComponentType());
                from.getBlock().methodReturn(newInstance(mktype, cast(paramType, arrayAtIndex(ref("keys"), constant(0)))));
            }
        } else {
            CodegenExpression[] expressions = new CodegenExpression[optionalMultiKeyClasses.getMKTypes().length];
            for (int i = 0; i < expressions.length; i++) {
                expressions[i] = cast(optionalMultiKeyClasses.getMKTypes()[i], arrayAtIndex(ref("keys"), constant(i)));
            }
            from.getBlock().methodReturn(newInstance(optionalMultiKeyClasses.getClassNameMK(), expressions));
        }
        return fromClass;
    }

    public static CodegenExpression codegenMultiKeyFromMultiKeyTransform(MultiKeyClassRef optionalMultiKeyClasses, CodegenMethod method, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass fromClass = newAnonymousClass(method.getBlock(), MultiKeyFromMultiKey.class);
        CodegenMethod from = CodegenMethod.makeParentNode(Object.class, MultiKeyCodegen.class, classScope).addParam(Object.class, "key");
        fromClass.addMethod("from", from);

        if (optionalMultiKeyClasses == null || optionalMultiKeyClasses.getClassNameMK() == null || optionalMultiKeyClasses.getMKTypes().length == 1) {
            from.getBlock().methodReturn(ref("key"));
        } else {
            CodegenExpression[] expressions = new CodegenExpression[optionalMultiKeyClasses.getMKTypes().length];
            from.getBlock().declareVar(MultiKey.class, "mk", cast(MultiKey.class, ref("key")));
            for (int i = 0; i < expressions.length; i++) {
                expressions[i] = cast(optionalMultiKeyClasses.getMKTypes()[i], exprDotMethod(ref("mk"), "getKey", constant(i)));
            }
            from.getBlock().methodReturn(newInstance(optionalMultiKeyClasses.getClassNameMK(), expressions));
        }
        return fromClass;
    }

    public static CodegenExpression codegenGetterEventDistinct(boolean isDistinct, EventType eventType, MultiKeyClassRef optionalDistinctMultiKey, CodegenMethod method, CodegenClassScope classScope) {
        if (!isDistinct) {
            return constantNull();
        }
        String[] propertyNames = eventType.getPropertyNames();
        EventTypeSPI spi = (EventTypeSPI) eventType;
        if (propertyNames.length == 1) {
            String propertyName = propertyNames[0];
            Class result = eventType.getPropertyType(propertyName);
            EventPropertyGetterSPI getter = spi.getGetterSPI(propertyName);
            return EventTypeUtility.codegenGetterWCoerceWArray(getter, result, null, method, MultiKeyCodegen.class, classScope);
        }
        EventPropertyGetterSPI[] getters = new EventPropertyGetterSPI[propertyNames.length];
        Class[] getterResultTypes = new Class[propertyNames.length];
        for (int i = 0; i < propertyNames.length; i++) {
            getterResultTypes[i] = eventType.getPropertyType(propertyNames[i]);
            getters[i] = spi.getGetterSPI(propertyNames[i]);
        }
        if (eventType instanceof VariantEventType) {
            return codegenMultikeyGetterBeanGet(getters, getterResultTypes, optionalDistinctMultiKey, method, classScope);
        }
        return codegenGetterMayMultiKey(eventType, getters, getterResultTypes, null, optionalDistinctMultiKey, method, classScope);
    }

    private static CodegenExpression codegenMultiKeyExprEvaluator(ExprForge[] expressionNodes, MultiKeyClassRef multiKeyClassRef, CodegenMethod method, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass evaluator = newAnonymousClass(method.getBlock(), ExprEvaluator.class);
        CodegenMethod evaluate = CodegenMethod.makeParentNode(Object.class, StmtClassForgeableMultiKey.class, classScope).addParam(ExprForgeCodegenNames.PARAMS);
        evaluator.addMethod("evaluate", evaluate);

        ExprForgeCodegenSymbol exprSymbol = new ExprForgeCodegenSymbol(true, null);
        CodegenMethod exprMethod = evaluate.makeChildWithScope(Object.class, CodegenLegoMethodExpression.class, exprSymbol, classScope).addParam(ExprForgeCodegenNames.PARAMS);

        CodegenExpression[] expressions = new CodegenExpression[expressionNodes.length];
        for (int i = 0; i < expressionNodes.length; i++) {
            expressions[i] = codegenExpressionMayCoerce(expressionNodes[i], multiKeyClassRef.getMKTypes()[i], exprMethod, exprSymbol, classScope);
        }
        exprSymbol.derivedSymbolsCodegen(evaluate, exprMethod.getBlock(), classScope);
        exprMethod.getBlock().methodReturn(newInstance(multiKeyClassRef.getClassNameMK(), expressions));

        evaluate.getBlock().methodReturn(localMethod(exprMethod, REF_EPS, REF_ISNEWDATA, REF_EXPREVALCONTEXT));
        return evaluator;
    }

    private static CodegenExpression codegenMultiKeyGetter(EventType eventType, EventPropertyGetterSPI[] getters, Class[] getterResultTypes, MultiKeyClassRef multiKeyClassRef, CodegenMethod method, CodegenClassScope classScope) {
        CodegenMethod get = CodegenMethod.makeParentNode(Object.class, StmtClassForgeableMultiKey.class, classScope).addParam(EventBean.class, "bean");
        CodegenExpressionNewAnonymousClass getter = newAnonymousClass(method.getBlock(), EventPropertyValueGetter.class);
        getter.addMethod("get", get);

        CodegenExpression[] expressions = new CodegenExpression[getters.length];
        for (int i = 0; i < getters.length; i++) {
            expressions[i] = getters[i].underlyingGetCodegen(ref("und"), get, classScope);
            Class mkType = multiKeyClassRef.getMKTypes()[i];
            Class getterType = getterResultTypes[i];
            expressions[i] = ExprNodeUtilityCodegen.codegenCoerce(expressions[i], getterType, mkType, true);
        }
        get.getBlock()
            .declareVar(eventType.getUnderlyingType(), "und", cast(eventType.getUnderlyingType(), exprDotUnderlying(ref("bean"))))
            .methodReturn(newInstance(multiKeyClassRef.getClassNameMK(), expressions));

        return getter;
    }

    private static CodegenExpression codegenMultikeyGetterBeanGet(EventPropertyGetterSPI[] getters, Class[] getterResultTypes, MultiKeyClassRef multiKeyClassRef, CodegenMethod method, CodegenClassScope classScope) {
        CodegenMethod get = CodegenMethod.makeParentNode(Object.class, StmtClassForgeableMultiKey.class, classScope).addParam(EventBean.class, "bean");
        CodegenExpressionNewAnonymousClass getter = newAnonymousClass(method.getBlock(), EventPropertyValueGetter.class);
        getter.addMethod("get", get);

        CodegenExpression[] expressions = new CodegenExpression[getters.length];
        for (int i = 0; i < getters.length; i++) {
            expressions[i] = getters[i].eventBeanGetCodegen(ref("bean"), get, classScope);
            Class mkType = multiKeyClassRef.getMKTypes()[i];
            Class getterType = getterResultTypes[i];
            expressions[i] = ExprNodeUtilityCodegen.codegenCoerce(expressions[i], getterType, mkType, true);
        }
        get.getBlock()
            .methodReturn(newInstance(multiKeyClassRef.getClassNameMK(), expressions));

        return getter;
    }
}
