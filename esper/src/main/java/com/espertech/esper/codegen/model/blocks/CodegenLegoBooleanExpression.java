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
package com.espertech.esper.codegen.model.blocks;

import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.method.CodegenParamSetExprPremade;
import com.espertech.esper.epl.expression.core.ExprForge;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class CodegenLegoBooleanExpression {

    private final static String PASS_NAME = "pass";

    /**
     * Generates code like this (premade expr assumed):
     * boolean/Boolean result = expression.evaluate(eps, isNewData, context);
     * if (result == null (optional early exit if null)  ||   (!? (Boolean) result)) {
     * return false/true;
     * }
     *
     * @param block   block
     * @param forge   forge
     * @param context context
     */
    public static void codegenReturnBoolIfNullOrBool(CodegenBlock block, ExprForge forge, CodegenContext context, boolean earlyExitIfNull, Boolean resultEarlyExit, boolean checkFor, boolean resultIfCheckPasses) {
        Class type = forge.getEvaluationType();
        if (type != boolean.class && type != Boolean.class) {
            throw new IllegalStateException("Invalid non-boolean expression");
        }
        block.declareVar(type, PASS_NAME, forge.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context));
        CodegenExpression passCheck = notOptional(!checkFor, ref(PASS_NAME));

        if (type.isPrimitive()) {
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
     * boolean/Boolean result = expression.evaluate(eps, isNewData, context);
     * if (result == null || (!(Boolean) result)) {
     * return value;
     * }
     *
     * @param block   block
     * @param forge   forge
     * @param context context
     */
    public static void codegenReturnValueIfNullOrNotPass(CodegenBlock block, ExprForge forge, CodegenContext context, CodegenExpression value) {
        codegenDOIfNullOrNotPass(block, forge, context, false, false, true, value);
    }

    /**
     * Generates code like this (premade expr assumed):
     * boolean/Boolean result = expression.evaluate(eps, isNewData, context);
     * if (result == null || (!(Boolean) result)) {
     * break;
     * }
     *
     * @param block   block
     * @param forge   forge
     * @param context context
     */
    public static void codegenBreakIfNullOrNotPass(CodegenBlock block, ExprForge forge, CodegenContext context) {
        codegenDOIfNullOrNotPass(block, forge, context, false, true, false, null);
    }

    /**
     * Generates code like this (premade expr assumed):
     * if (pass == null || (!(Boolean) pass)) {
     * continue;
     * }
     *
     * @param block   block
     * @param forge   forge
     * @param context context
     */
    public static void codegenContinueIfNullOrNotPass(CodegenBlock block, ExprForge forge, CodegenContext context) {
        codegenDOIfNullOrNotPass(block, forge, context, true, false, false, null);
    }

    private static void codegenDOIfNullOrNotPass(CodegenBlock block, ExprForge forge, CodegenContext context, boolean doContinue, boolean doBreakLoop, boolean doReturn, CodegenExpression returnValue) {
        Class type = forge.getEvaluationType();
        if (type != boolean.class && type != Boolean.class) {
            throw new IllegalStateException("Invalid non-boolean expression");
        }
        block.declareVar(type, PASS_NAME, forge.evaluateCodegen(CodegenParamSetExprPremade.INSTANCE, context));
        CodegenExpression passCheck = not(ref(PASS_NAME));

        CodegenExpression condition;
        if (type.isPrimitive()) {
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
}
