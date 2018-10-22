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
package com.espertech.esper.common.internal.epl.expression.codegen;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class CodegenLegoBooleanExpression {

    private final static String PASS_NAME = "pass";

    /**
     * Generates code like this (premade expr assumed):
     * <pre>
     * boolean/Boolean result = expression.evaluate(eps, isNewData, context);
     * if (result == null (optional early exit if null)  ||   (!? (Boolean) result)) {
     * return false/true;
     * }
     * </pre>
     *
     * @param block               block
     * @param earlyExitIfNull     indicator
     * @param resultEarlyExit     indicator
     * @param checkFor            indicator
     * @param resultIfCheckPasses indicator
     * @param evaluationType      type
     * @param expression          expr
     */
    public static void codegenReturnBoolIfNullOrBool(CodegenBlock block, Class evaluationType, CodegenExpression expression, boolean earlyExitIfNull, Boolean resultEarlyExit, boolean checkFor, boolean resultIfCheckPasses) {
        if (evaluationType != boolean.class && evaluationType != Boolean.class) {
            throw new IllegalStateException("Invalid non-boolean expression");
        }
        block.declareVar(evaluationType, PASS_NAME, expression);
        CodegenExpression passCheck = notOptional(!checkFor, ref(PASS_NAME));

        if (evaluationType.isPrimitive()) {
            block.ifCondition(passCheck).blockReturn(constant(resultIfCheckPasses));
            return;
        }

        if (earlyExitIfNull) {
            if (resultEarlyExit == null) {
                throw new IllegalStateException("Invalid null for result-early-exit");
            }
            block.ifRefNull(PASS_NAME).blockReturn(constant(resultEarlyExit));
            block.ifCondition(passCheck).blockReturn(constant(resultIfCheckPasses));
            return;
        }

        block.ifCondition(and(notEqualsNull(ref(PASS_NAME)), passCheck)).blockReturn(constant(resultIfCheckPasses));
    }

    /**
     * Generates code like this (premade expr assumed):
     * <pre>
     * boolean/Boolean result = expression.evaluate(eps, isNewData, context);
     * if (result != null &amp;&amp; (!(Boolean) result)) {
     * return value;
     * }
     * </pre>
     *
     * @param block          block
     * @param evaluationType eval type
     * @param expression     expression
     * @param value          value
     */
    public static void codegenReturnValueIfNotNullAndNotPass(CodegenBlock block, Class evaluationType, CodegenExpression expression, CodegenExpression value) {
        codegenDoIfNotNullAndNotPass(block, evaluationType, expression, false, false, value);
    }

    /**
     * Generates code like this (premade expr assumed):
     * <pre>
     * boolean/Boolean result = expression.evaluate(eps, isNewData, context);
     * if (result == null || (!(Boolean) result)) {
     * return value;
     * }
     * </pre>
     *
     * @param block          block
     * @param evaluationType eval type
     * @param expression     expression
     * @param value          value
     */
    public static void codegenReturnValueIfNullOrNotPass(CodegenBlock block, Class evaluationType, CodegenExpression expression, CodegenExpression value) {
        codegenDoIfNullOrNotPass(block, evaluationType, expression, false, false, value);
    }

    /**
     * Generates code like this (premade expr assumed):
     * <pre>
     * boolean/Boolean result = expression.evaluate(eps, isNewData, context);
     * if (result == null || (!(Boolean) result)) {
     * break;
     * }
     * </pre>
     *
     * @param block          block
     * @param evaluationType eval type
     * @param expression     expression
     */
    public static void codegenBreakIfNotNullAndNotPass(CodegenBlock block, Class evaluationType, CodegenExpression expression) {
        codegenDoIfNotNullAndNotPass(block, evaluationType, expression, false, true, constantNull());
    }

    /**
     * Generates code like this (premade expr assumed):
     * <pre>
     * if (pass != null &amp;&amp; (!(Boolean) pass)) {
     * continue;
     * }
     * </pre>
     *
     * @param block          block
     * @param evaluationType eval type
     * @param expression     expression
     */
    public static void codegenContinueIfNotNullAndNotPass(CodegenBlock block, Class evaluationType, CodegenExpression expression) {
        codegenDoIfNotNullAndNotPass(block, evaluationType, expression, true, false, constantNull());
    }

    /**
     * Generates code like this (premade expr assumed):
     * <pre>
     * if (pass == null || (!(Boolean) pass)) {
     * continue;
     * }
     * </pre>
     *
     * @param block          block
     * @param evaluationType eval type
     * @param expression     expression
     */
    public static void codegenContinueIfNullOrNotPass(CodegenBlock block, Class evaluationType, CodegenExpression expression) {
        codegenDoIfNullOrNotPass(block, evaluationType, expression, true, false, constantNull());
    }

    private static void codegenDoIfNotNullAndNotPass(CodegenBlock block, Class evaluationType, CodegenExpression expression, boolean doContinue, boolean doBreakLoop, CodegenExpression returnValue) {
        if (evaluationType != boolean.class && evaluationType != Boolean.class) {
            throw new IllegalStateException("Invalid non-boolean expression");
        }
        block.declareVar(evaluationType, PASS_NAME, expression);
        CodegenExpression passCheck = not(ref(PASS_NAME));

        CodegenExpression condition;
        if (evaluationType.isPrimitive()) {
            condition = passCheck;
        } else {
            condition = and(notEqualsNull(ref(PASS_NAME)), passCheck);
        }

        if (doContinue) {
            block.ifCondition(condition).blockContinue();
        } else if (doBreakLoop) {
            block.ifCondition(condition).breakLoop();
        } else {
            block.ifCondition(condition).blockReturn(returnValue);
        }
    }

    private static void codegenDoIfNullOrNotPass(CodegenBlock block, Class evaluationType, CodegenExpression expression, boolean doContinue, boolean doBreakLoop, CodegenExpression returnValue) {
        if (evaluationType != boolean.class && evaluationType != Boolean.class) {
            throw new IllegalStateException("Invalid non-boolean expression");
        }
        block.declareVar(evaluationType, PASS_NAME, expression);
        CodegenExpression passCheck = not(ref(PASS_NAME));

        CodegenExpression condition;
        if (evaluationType.isPrimitive()) {
            condition = passCheck;
        } else {
            condition = or(equalsNull(ref(PASS_NAME)), passCheck);
        }

        if (doContinue) {
            block.ifCondition(condition).blockContinue();
        } else if (doBreakLoop) {
            block.ifCondition(condition).breakLoop();
        } else {
            block.ifCondition(condition).blockReturn(returnValue);
        }
    }
}
