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
package com.espertech.esper.regressionrun.suite.rowrecog;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.suite.rowrecog.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionlib.support.rowrecog.SupportRecogBean;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteRowRecog extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testRowRecogOps() {
        RegressionRunner.run(session, RowRecogOps.executions());
    }

    public void testRowRecogAfter() {
        RegressionRunner.run(session, RowRecogAfter.executions());
    }

    public void testRowRecogInvalid() {
        RegressionRunner.run(session, new RowRecogInvalid());
    }

    public void testRowRecogClausePresence() {
        RegressionRunner.run(session, new RowRecogClausePresence());
    }

    public void testRowRecogGreedyness() {
        RegressionRunner.run(session, RowRecogGreedyness.executions());
    }

    public void testRowRecogEmptyPartition() {
        RegressionRunner.run(session, new RowRecogEmptyPartition());
    }

    public void testRowRecogEnumMethod() {
        RegressionRunner.run(session, new RowRecogEnumMethod());
    }

    public void testRowRecogIntervalResolution() {
        RegressionRunner.run(session, new RowRecogIntervalResolution(10000));
    }

    public void testRowRecogIterateOnly() {
        RegressionRunner.run(session, RowRecogIterateOnly.executions());
    }

    public void testRowRecogPerf() {
        RegressionRunner.run(session, new RowRecogPerf());
    }

    public void testRowRecogPermute() {
        RegressionRunner.run(session, new RowRecogPermute());
    }

    public void testRowRecogRegex() {
        RegressionRunner.run(session, new RowRecogRegex());
    }

    public void testRowRecogPrev() {
        RegressionRunner.run(session, RowRecogPrev.executions());
    }

    public void testRowRecogDataWin() {
        RegressionRunner.run(session, RowRecogDataWin.executions());
    }

    public void testRowRecogDelete() {
        RegressionRunner.run(session, RowRecogDelete.executions());
    }

    public void testRowRecogRepetition() {
        RegressionRunner.run(session, new RowRecogRepetition());
    }

    public void testRowRecogAggregation() {
        RegressionRunner.run(session, RowRecogAggregation.executions());
    }

    public void testRowRecogInterval() {
        RegressionRunner.run(session, RowRecogInterval.executions());
    }

    public void testRowRecogIntervalOrTerminated() {
        RegressionRunner.run(session, new RowRecogIntervalOrTerminated());
    }

    public void testRowRecogVariantStream() {
        RegressionRunner.run(session, new RowRecogVariantStream());
    }

    public void testRowRecogArrayAccess() {
        RegressionRunner.run(session, RowRecogArrayAccess.executions());
    }

    public void testRowRecogDataSet() {
        RegressionRunner.run(session, RowRecogDataSet.executions());
    }

    public void testRowRecogMultikeyWArray() {
        RegressionRunner.run(session, RowRecogMultikeyWArray.executions());
    }

    private void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class,
            SupportRecogBean.class, SupportBean_A.class, SupportBean_B.class, SupportEventWithIntArray.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        configuration.getCommon().addEventType("TemperatureSensorEvent",
            "id,device,temp".split(","), new Object[]{String.class, int.class, double.class});

        configuration.getCommon().addImport(SupportStaticMethodLib.class.getName());
        configuration.getCommon().addVariable("mySleepDuration", long.class, 100);    // msec
    }
}
