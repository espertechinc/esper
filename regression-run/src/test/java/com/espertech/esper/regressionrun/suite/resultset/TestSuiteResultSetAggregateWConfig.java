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
package com.espertech.esper.regressionrun.suite.resultset;

import com.espertech.esper.regressionlib.suite.resultset.aggregate.ResultSetAggregateExtInvalid;
import com.espertech.esper.regressionlib.suite.resultset.aggregate.ResultSetAggregateFilteredWMathContext;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanNumeric;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.math.MathContext;
import java.math.RoundingMode;

public class TestSuiteResultSetAggregateWConfig extends TestCase {
    public void testResultSetAggregateFilteredWMathContext() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCompiler().getExpression().setMathContext(new MathContext(2, RoundingMode.HALF_UP));
        session.getConfiguration().getCommon().addEventType(SupportBeanNumeric.class);
        RegressionRunner.run(session, new ResultSetAggregateFilteredWMathContext());
        session.destroy();
    }

    public void testResultSetAggregateExtInvalid() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCompiler().getExpression().setExtendedAggregation(false);
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        RegressionRunner.run(session, new ResultSetAggregateExtInvalid());
        session.destroy();
    }
}