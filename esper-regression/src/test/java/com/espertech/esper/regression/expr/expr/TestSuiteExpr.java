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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteExpr extends TestCase {
    public void testExecExprAnyAllSomeExpr() {
        RegressionRunner.run(new ExecExprAnyAllSomeExpr());
    }

    public void testExecExprArrayExpression() {
        RegressionRunner.run(new ExecExprArrayExpression());
    }

    public void testExecExprBigNumberSupport() {
        RegressionRunner.run(new ExecExprBigNumberSupport());
    }

    public void testExecExprBigNumberSupportMathContext() {
        RegressionRunner.run(new ExecExprBigNumberSupportMathContext());
    }

    public void testExecExprBitWiseOperators() {
        RegressionRunner.run(new ExecExprBitWiseOperators());
    }

    public void testExecExprCaseExpr() {
        RegressionRunner.run(new ExecExprCaseExpr());
    }

    public void testExecExprCast() {
        RegressionRunner.run(new ExecExprCast());
    }

    public void testExecExprCastWStaticType() {
        RegressionRunner.run(new ExecExprCastWStaticType());
    }

    public void testExecExprCurrentEvaluationContext() {
        RegressionRunner.run(new ExecExprCurrentEvaluationContext());
    }

    public void testExecExprCurrentTimestamp() {
        RegressionRunner.run(new ExecExprCurrentTimestamp());
    }

    public void testExecExprDotExpression() {
        RegressionRunner.run(new ExecExprDotExpression());
    }

    public void testExecExprDotExpressionDuckTyping() {
        RegressionRunner.run(new ExecExprDotExpressionDuckTyping());
    }

    public void testExecExprExists() {
        RegressionRunner.run(new ExecExprExists());
    }

    public void testExecExprInBetweenLike() {
        RegressionRunner.run(new ExecExprInBetweenLike());
    }

    public void testExecExprInstanceOf() {
        RegressionRunner.run(new ExecExprInstanceOf());
    }

    public void testExecExprLikeRegexp() {
        RegressionRunner.run(new ExecExprLikeRegexp());
    }

    public void testExecExprMath() {
        RegressionRunner.run(new ExecExprMath());
    }

    public void testExecExprMathDivisionRules() {
        RegressionRunner.run(new ExecExprMathDivisionRules());
    }

    public void testExecExprNewInstance() {
        RegressionRunner.run(new ExecExprNewInstance());
    }

    public void testExecExprNewStruct() {
        RegressionRunner.run(new ExecExprNewStruct());
    }

    public void testExecExprConcat() {
        RegressionRunner.run(new ExecExprConcat());
    }

    public void testExecExprCoalesce() {
        RegressionRunner.run(new ExecExprCoalesce());
    }

    public void testExecExprOpModulo() {
        RegressionRunner.run(new ExecExprOpModulo());
    }

    public void testExecExprPrevious() {
        RegressionRunner.run(new ExecExprPrevious());
    }

    public void testExecExprPrior() {
        RegressionRunner.run(new ExecExprPrior());
    }

    public void testExecExprRelOp() {
        RegressionRunner.run(new ExecExprRelOp());
    }

    public void testExecExprMinMaxNonAgg() {
        RegressionRunner.run(new ExecExprMinMaxNonAgg());
    }

    public void testExecExprTypeOf() {
        RegressionRunner.run(new ExecExprTypeOf());
    }

    public void testExecExprEqualsIs() {
        RegressionRunner.run(new ExecExprEqualsIs());
    }

    public void testExecExprAndOrNot() {
        RegressionRunner.run(new ExecExprAndOrNot());
    }
}
