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
package com.espertech.esper.regressionrun.suite.view;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.suite.view.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteView extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testViewLengthWin() {
        RegressionRunner.run(session, ViewLengthWin.executions());
    }

    public void testViewTimeWin() {
        RegressionRunner.run(session, ViewTimeWin.executions());
    }

    public void testViewKeepAll() {
        RegressionRunner.run(session, ViewKeepAll.executions());
    }

    public void testViewTimeBatch() {
        RegressionRunner.run(session, ViewTimeBatch.executions());
    }

    public void testViewTimeLengthBatch() {
        RegressionRunner.run(session, ViewTimeLengthBatch.executions());
    }

    public void testViewLengthBatch() {
        RegressionRunner.run(session, ViewLengthBatch.executions());
    }

    public void testViewSort() {
        RegressionRunner.run(session, ViewSort.executions());
    }

    public void testViewRank() {
        RegressionRunner.run(session, ViewRank.executions());
    }

    public void testViewTimeAccum() {
        RegressionRunner.run(session, ViewTimeAccum.executions());
    }

    public void testViewUnique() {
        RegressionRunner.run(session, ViewUnique.executions());
    }

    public void testViewMultikeyWArray() {
        RegressionRunner.run(session, ViewMultikeyWArray.executions());
    }

    public void testViewFirstUnique() {
        RegressionRunner.run(session, ViewFirstUnique.executions());
    }

    public void testViewFirstTime() {
        RegressionRunner.run(session, ViewFirstTime.executions());
    }

    public void testViewTimeOrderAndTimeToLive() {
        RegressionRunner.run(session, ViewTimeOrderAndTimeToLive.executions());
    }

    public void testViewExternallyBatched() {
        RegressionRunner.run(session, ViewExternallyTimedBatched.executions());
    }

    public void testViewExternallyTimedWin() {
        RegressionRunner.run(session, ViewExternallyTimedWin.executions());
    }

    public void testViewLastEvent() {
        RegressionRunner.run(session, ViewLastEvent.executions());
    }

    public void testViewFirstEvent() {
        RegressionRunner.run(session, ViewFirstEvent.executions());
    }

    public void testViewFirstLength() {
        RegressionRunner.run(session, ViewFirstLength.executions());
    }

    public void testViewDerived() {
        RegressionRunner.run(session, ViewDerived.executions());
    }

    public void testViewGroup() {
        RegressionRunner.run(session, ViewGroup.executions());
    }

    public void testViewIntersect() {
        RegressionRunner.run(session, ViewIntersect.executions());
    }

    public void testViewUnion() {
        RegressionRunner.run(session, ViewUnion.executions());
    }

    public void testViewExpressionBatch() {
        RegressionRunner.run(session, ViewExpressionBatch.executions());
    }

    public void testViewExpressionWindow() {
        RegressionRunner.run(session, ViewExpressionWindow.executions());
    }

    public void testViewInvalid() {
        RegressionRunner.run(session, new ViewInvalid());
    }

    public void testViewParameterizedByContext() {
        RegressionRunner.run(session, ViewParameterizedByContext.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportMarketDataBean.class, SupportBeanComplexProps.class, SupportBean.class,
            SupportBeanWithEnum.class, SupportBeanTimestamp.class, SupportEventIdWithTimestamp.class, SupportSensorEvent.class,
            SupportBean_S0.class, SupportBean_S1.class, SupportBean_A.class, SupportBean_N.class, SupportContextInitEventWLength.class,
            SupportEventWithLongArray.class, SupportObjectArrayOneDim.class}) {
            configuration.getCommon().addEventType(clazz.getSimpleName(), clazz);
        }
        configuration.getCommon().addEventType("OAEventStringInt", new String[]{"p1", "p2"}, new Object[]{String.class, int.class});

        configuration.getCommon().addVariable("TIME_WIN_ONE", int.class, 4);
        configuration.getCommon().addVariable("TIME_WIN_TWO", double.class, 4000);

        configuration.getCompiler().getByteCode().setThreadPoolCompilerNumThreads(0);
        configuration.getCompiler().addPlugInSingleRowFunction("udf", ViewExpressionWindow.LocalUDF.class.getName(), "evaluateExpiryUDF");

        configuration.getCommon().addImport(DefaultSupportSourceOp.class.getPackage().getName() + ".*");
        configuration.getCommon().addImport(DefaultSupportCaptureOp.class.getPackage().getName() + ".*");
    }
}
