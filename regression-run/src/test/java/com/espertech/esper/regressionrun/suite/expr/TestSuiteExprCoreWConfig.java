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
package com.espertech.esper.regressionrun.suite.expr;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.regressionlib.suite.expr.exprcore.ExprCoreBigNumberSupportMathContext;
import com.espertech.esper.regressionlib.suite.expr.exprcore.ExprCoreDotExpressionDuckTyping;
import com.espertech.esper.regressionlib.suite.expr.exprcore.ExprCoreMathDivisionRules;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanDuckType;
import com.espertech.esper.regressionlib.support.bean.SupportBeanDuckTypeOne;
import com.espertech.esper.regressionlib.support.bean.SupportBeanDuckTypeTwo;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.math.MathContext;

public class TestSuiteExprCoreWConfig extends TestCase {

    public void testExprCoreDotExpressionDuckTyping() {
        RegressionSession session = RegressionRunner.session();
        Configuration configuration = session.getConfiguration();
        configuration.getCompiler().getExpression().setDuckTyping(true);
        configuration.getCommon().addEventType(SupportBeanDuckType.class);
        configuration.getCommon().addEventType(SupportBeanDuckTypeOne.class);
        configuration.getCommon().addEventType(SupportBeanDuckTypeTwo.class);
        RegressionRunner.run(session, new ExprCoreDotExpressionDuckTyping());
        session.destroy();
    }

    public void testExprCoreBigNumberSupportMathContext() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        session.getConfiguration().getCompiler().getExpression().setMathContext(MathContext.DECIMAL32);
        session.getConfiguration().getCompiler().getByteCode().setAllowSubscriber(true);
        RegressionRunner.run(session, ExprCoreBigNumberSupportMathContext.executions());
        session.destroy();
    }

    public void testExprCoreMathDivisionRules() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCompiler().getExpression().setIntegerDivision(true);
        session.getConfiguration().getCompiler().getExpression().setDivisionByZeroReturnsNull(true);
        session.getConfiguration().getCommon().addEventType("SupportBean", SupportBean.class);
        RegressionRunner.run(session, ExprCoreMathDivisionRules.executions());
        session.destroy();
    }
}
