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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommon;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.suite.resultset.outputlimit.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.extend.aggfunc.SupportInvocationCountForge;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteResultSetOutputLimit extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testResultSetOutputLimitSimple() {
        RegressionRunner.run(session, ResultSetOutputLimitSimple.executions());
    }

    public void testResultSetOutputLimitRowForAll() {
        RegressionRunner.run(session, ResultSetOutputLimitRowForAll.executions());
    }

    public void testResultSetOutputLimitRowPerEvent() {
        RegressionRunner.run(session, ResultSetOutputLimitRowPerEvent.executions());
    }

    public void testResultSetOutputLimitRowPerGroup() {
        RegressionRunner.run(session, ResultSetOutputLimitRowPerGroup.executions());
    }

    public void testResultSetOutputLimitAggregateGrouped() {
        RegressionRunner.run(session, ResultSetOutputLimitAggregateGrouped.executions());
    }

    public void testResultSetOutputLimitRowPerGroupRollup() {
        RegressionRunner.run(session, ResultSetOutputLimitRowPerGroupRollup.executions());
    }

    public void testResultSetOutputLimitRowLimit() {
        RegressionRunner.run(session, ResultSetOutputLimitRowLimit.executions());
    }

    public void testResultSetOutputLimitChangeSetOpt() {
        RegressionRunner.run(session, new ResultSetOutputLimitChangeSetOpt(true));
    }

    public void testResultSetOutputLimitFirstHaving() {
        RegressionRunner.run(session, ResultSetOutputLimitFirstHaving.executions());
    }

    public void testResultSetOutputLimitCrontabWhen() {
        RegressionRunner.run(session, ResultSetOutputLimitCrontabWhen.executions());
    }

    public void testResultSetOutputLimitMicrosecondResolution() {
        RegressionRunner.run(session, new ResultSetOutputLimitMicrosecondResolution(0, "1", 1000, 1000));
        RegressionRunner.run(session, new ResultSetOutputLimitMicrosecondResolution(789123456789L, "0.1", 789123456789L + 100, 100));
    }

    public void testResultSetOutputLimitParameterizedByContext() {
        RegressionRunner.run(session, new ResultSetOutputLimitParameterizedByContext());
    }

    public void testResultSetOutputLimitAfter() {
        RegressionRunner.run(session, ResultSetOutputLimitAfter.executions());
    }

    public void testResultSetOutputLimitInsertInto() {
        RegressionRunner.run(session, ResultSetOutputLimitInsertInto.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class,
            SupportMarketDataBean.class, SupportBeanNumeric.class, SupportBean_ST0.class, SupportBean_A.class,
            SupportScheduleSimpleEvent.class, SupportBeanString.class, SupportEventWithIntArray.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        ConfigurationCommon common = configuration.getCommon();
        common.addVariable("D", int.class, 1);
        common.addVariable("H", int.class, 2);
        common.addVariable("M", int.class, 3);
        common.addVariable("S", int.class, 4);
        common.addVariable("MS", int.class, 5);

        common.addVariable("varoutone", boolean.class, false);
        common.addVariable("myint", int.class, 0);
        common.addVariable("mystring", String.class, "");
        common.addVariable("myvar", int.class, 0);
        common.addVariable("count_insert_var", int.class, 0);
        common.addVariable("myvardummy", int.class, 0);
        common.addVariable("myvarlong", long.class, 0);

        configuration.getCompiler().getByteCode().setAllowSubscriber(true);
        configuration.getCompiler().addPlugInAggregationFunctionForge("customagg", SupportInvocationCountForge.class.getName());
    }
}
