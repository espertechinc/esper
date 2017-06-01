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
package com.espertech.esper.codegen.model.expression;

import java.util.Map;
import java.util.Set;

public class CodegenExpressionBuilder {
    public static CodegenExpression ref(String ref) {
        return new CodegenExpressionRef(ref);
    }

    public static CodegenExpressionExprDotName exprDotName(CodegenExpression left, String name) {
        return new CodegenExpressionExprDotName(left, name);
    }

    public static CodegenExpression exprDotMethod(CodegenExpression expression, String method, CodegenExpression... params) {
        return new CodegenExpressionExprDotMethod(expression, method, params);
    }

    public static CodegenExpressionExprDotMethodChain exprDotMethodChain(CodegenExpression expression) {
        return new CodegenExpressionExprDotMethodChain(expression);
    }

    public static CodegenExpression exprDotUnderlying(CodegenExpression expression) {
        return new CodegenExpressionExprDotUnderlying(expression);
    }

    public static CodegenExpression beanUndCastDotMethodConst(Class clazz, CodegenExpression beanExpression, String method, String constant) {
        return new CodegenExpressionBeanUndCastDotMethodConst(clazz, beanExpression, method, constant);
    }

    public static CodegenExpression beanUndCastArrayAtIndex(Class clazz, CodegenExpression beanExpression, int index) {
        return new CodegenExpressionBeanUndCastArrayAtIndex(clazz, beanExpression, index);
    }

    public static CodegenExpression localMethod(String methodName, CodegenExpression expression) {
        return new CodegenExpressionLocalMethod(methodName, expression);
    }

    public static CodegenExpression constantTrue() {
        return CodegenExpressionConstantTrue.INSTANCE;
    }

    public static CodegenExpression constantFalse() {
        return CodegenExpressionConstantFalse.INSTANCE;
    }

    public static CodegenExpression constantNull() {
        return CodegenExpressionConstantNull.INSTANCE;
    }

    public static CodegenExpression constant(Object constant) {
        return new CodegenExpressionConstant(constant);
    }

    public static CodegenExpression castUnderlying(Class clazz, CodegenExpression expression) {
        return new CodegenExpressionCastUnderlying(clazz, expression);
    }

    public static CodegenExpression instanceOf(CodegenExpression lhs, Class clazz) {
        return new CodegenExpressionInstanceOf(lhs, clazz, false);
    }

    public static CodegenExpression notInstanceOf(CodegenExpression lhs, Class clazz) {
        return new CodegenExpressionInstanceOf(lhs, clazz, true);
    }

    public static CodegenExpression castRef(Class clazz, String ref) {
        return new CodegenExpressionCastRef(clazz, ref);
    }

    public static CodegenExpression conditional(CodegenExpression condition, CodegenExpression expressionTrue, CodegenExpression expressionFalse) {
        return new CodegenExpressionConditional(condition, expressionTrue, expressionFalse);
    }

    public static CodegenExpression not(CodegenExpression expression) {
        return new CodegenExpressionNot(expression);
    }

    public static CodegenExpression cast(Class clazz, CodegenExpression expression) {
        return new CodegenExpressionCastExpression(clazz, expression);
    }

    public static CodegenExpression notEqualsNull(CodegenExpression lhs) {
        return new CodegenExpressionEqualsNull(lhs, true);
    }

    public static CodegenExpression equalsNull(CodegenExpression lhs) {
        return new CodegenExpressionEqualsNull(lhs, false);
    }

    public static CodegenExpression staticMethod(Class clazz, String method, String... refs) {
        return new CodegenExpressionStaticMethodTakingRefs(clazz, method, refs);
    }

    public static CodegenExpression staticMethod(Class clazz, String method, CodegenExpression... params) {
        return new CodegenExpressionStaticMethodTakingAny(clazz, method, params);
    }

    public static CodegenExpression staticMethodTakingExprAndConst(Class clazz, String method, CodegenExpression expression, Object... consts) {
        return new CodegenExpressionStaticMethodTakingExprAndConst(clazz, method, expression, consts);
    }

    public static CodegenExpression arrayAtIndex(CodegenExpression expression, CodegenExpression index) {
        return new CodegenExpressionArrayAtIndex(expression, index);
    }

    public static CodegenExpression arrayLength(CodegenExpression expression) {
        return new CodegenExpressionArrayLength(expression);
    }

    public static CodegenExpression newInstance(Class clazz, CodegenExpression... params) {
        return new CodegenExpressionNewInstance(clazz, params);
    }

    public static CodegenExpression relational(CodegenExpression lhs, CodegenExpressionRelational.CodegenRelational op, CodegenExpression rhs) {
        return new CodegenExpressionRelational(lhs, op, rhs);
    }

    public static CodegenExpression newArray(Class component, CodegenExpression expression) {
        return new CodegenExpressionNewArray(component, expression);
    }

    public static void renderExpressions(StringBuilder builder, CodegenExpression[] expressions, Map<Class, String> imports) {
        String delimiter = "";
        for (CodegenExpression expression : expressions) {
            builder.append(delimiter);
            expression.render(builder, imports);
            delimiter = ",";
        }
    }

    public static void mergeClassesExpressions(Set<Class> classes, CodegenExpression[] expressions) {
        for (CodegenExpression expression : expressions) {
            expression.mergeClasses(classes);
        }
    }
}
