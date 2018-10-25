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
import com.espertech.esper.regressionlib.suite.rowrecog.RowRecogIntervalResolution;
import com.espertech.esper.regressionlib.suite.rowrecog.RowRecogMaxStatesEngineWide3Instance;
import com.espertech.esper.regressionlib.suite.rowrecog.RowRecogMaxStatesEngineWide4Instance;
import com.espertech.esper.regressionlib.suite.rowrecog.RowRecogMaxStatesEngineWideNoPreventStart;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.support.client.SupportConditionHandlerFactory;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.concurrent.TimeUnit;

public class TestSuiteRowRecogWConfig extends TestCase {

    public void testRowRecogIntervalMicrosecondResolution() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        session.getConfiguration().getCommon().getTimeSource().setTimeUnit(TimeUnit.MICROSECONDS);
        RegressionRunner.run(session, new RowRecogIntervalResolution(10000000));
        session.destroy();
    }

    public void testRowRecogMaxStatesEngineWideNoPreventStart() {
        RegressionSession session = RegressionRunner.session();
        configure(session.getConfiguration());
        session.getConfiguration().getRuntime().getMatchRecognize().setMaxStates(3L);
        session.getConfiguration().getRuntime().getMatchRecognize().setMaxStatesPreventStart(false);
        RegressionRunner.run(session, new RowRecogMaxStatesEngineWideNoPreventStart());
        session.destroy();
    }

    public void testRowRecogMaxStatesEngineWide3Instance() {
        RegressionSession session = RegressionRunner.session();
        configure(session.getConfiguration());
        session.getConfiguration().getRuntime().getMatchRecognize().setMaxStates(3L);
        session.getConfiguration().getRuntime().getMatchRecognize().setMaxStatesPreventStart(true);
        RegressionRunner.run(session, new RowRecogMaxStatesEngineWide3Instance());
        session.destroy();
    }

    public void testRowRecogMaxStatesEngineWide4Instance() {
        RegressionSession session = RegressionRunner.session();
        configure(session.getConfiguration());
        session.getConfiguration().getRuntime().getMatchRecognize().setMaxStates(4L);
        session.getConfiguration().getRuntime().getMatchRecognize().setMaxStatesPreventStart(true);
        RegressionRunner.run(session, new RowRecogMaxStatesEngineWide4Instance());
        session.destroy();
    }

    private void configure(Configuration configuration) {
        configuration.getCommon().addEventType(SupportBean.class);
        configuration.getCommon().addEventType(SupportBean_S0.class);
        configuration.getCommon().addEventType(SupportBean_S1.class);
        configuration.getRuntime().getConditionHandling().addClass(SupportConditionHandlerFactory.class);
    }
}
