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
package com.espertech.esper.regression.epl.other;

import com.espertech.esper.supportregression.execution.RegressionRunner;
import junit.framework.TestCase;

public class TestSuiteOther extends TestCase {
    public void testExecEPLAsKeywordBacktick() {
        RegressionRunner.run(new ExecEPLAsKeywordBacktick());
    }

    public void testExecEPLComments() {
        RegressionRunner.run(new ExecEPLComments());
    }

    public void testExecEPLCreateExpression() {
        RegressionRunner.run(new ExecEPLCreateExpression());
    }

    public void testExecEPLDistinct() {
        RegressionRunner.run(new ExecEPLDistinct());
    }

    public void testExecEPLDistinctWildcardJoinPattern() {
        RegressionRunner.run(new ExecEPLDistinctWildcardJoinPattern());
    }

    public void testExecEPLForGroupDelivery() {
        RegressionRunner.run(new ExecEPLForGroupDelivery());
    }

    public void testExecEPLInvalid() {
        RegressionRunner.run(new ExecEPLInvalid());
    }

    public void testExecEPLIStreamRStreamKeywords() {
        RegressionRunner.run(new ExecEPLIStreamRStreamKeywords());
    }

    public void testExecEPLIStreamRStreamConfigSelectorRStream() {
        RegressionRunner.run(new ExecEPLIStreamRStreamConfigSelectorRStream());
    }

    public void testExecEPLIStreamRStreamConfigSelectorIRStream() {
        RegressionRunner.run(new ExecEPLIStreamRStreamConfigSelectorIRStream());
    }

    public void testExecEPLLiteralConstants() {
        RegressionRunner.run(new ExecEPLLiteralConstants());
    }

    public void testExecEPLSchema() {
        RegressionRunner.run(new ExecEPLSchema());
    }

    public void testExecEPLSplitStream() {
        RegressionRunner.run(new ExecEPLSplitStream());
    }

    public void testExecEPLUpdate() {
        RegressionRunner.run(new ExecEPLUpdate());
    }

    public void testExecEPLUpdateMapIndexProps() {
        RegressionRunner.run(new ExecEPLUpdateMapIndexProps());
    }

    public void testExecEPLStaticFunctions() {
        RegressionRunner.run(new ExecEPLStaticFunctions());
    }

    public void testExecEPLStaticFunctionsNoUDFCache() {
        RegressionRunner.run(new ExecEPLStaticFunctionsNoUDFCache());
    }

    public void testExecEPLSelectExpr() {
        RegressionRunner.run(new ExecEPLSelectExpr());
    }

    public void testExecEPLSelectWildcardWAdditional() {
        RegressionRunner.run(new ExecEPLSelectWildcardWAdditional());
    }

    public void testExecEPLSelectExprSQLCompat() {
        RegressionRunner.run(new ExecEPLSelectExprSQLCompat());
    }

    public void testExecEPLSelectExprEventBeanAnnotation() {
        RegressionRunner.run(new ExecEPLSelectExprEventBeanAnnotation());
    }

    public void testExecEPLSelectExprStreamSelector() {
        RegressionRunner.run(new ExecEPLSelectExprStreamSelector());
    }

    public void testExecEPLPlanExcludeHint() {
        RegressionRunner.run(new ExecEPLPlanExcludeHint());
    }

    public void testExecEPLPlanInKeywordQuery() {
        RegressionRunner.run(new ExecEPLPlanInKeywordQuery());
    }

    public void testExecEPLStreamExpr() {
        RegressionRunner.run(new ExecEPLStreamExpr());
    }

    public void testExecEPLSelectJoin() {
        RegressionRunner.run(new ExecEPLSelectJoin());
    }

    public void testExecEPLPatternEventProperties() {
        RegressionRunner.run(new ExecEPLPatternEventProperties());
    }

    public void testExecEPLPatternQueries() {
        RegressionRunner.run(new ExecEPLPatternQueries());
    }
}
