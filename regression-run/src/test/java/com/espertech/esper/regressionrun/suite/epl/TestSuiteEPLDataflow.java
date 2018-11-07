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
package com.espertech.esper.regressionrun.suite.epl;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportCaptureOp;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportGraphEventUtil;
import com.espertech.esper.common.internal.epl.dataflow.util.DefaultSupportSourceOp;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.suite.epl.dataflow.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.dataflow.MyLineFeedSource;
import com.espertech.esper.regressionlib.support.dataflow.MyObjectArrayGraphSource;
import com.espertech.esper.regressionlib.support.dataflow.MyTokenizerCounter;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteEPLDataflow extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEPLDataflowOpBeaconSource() {
        RegressionRunner.run(session, EPLDataflowOpBeaconSource.executions());
    }

    public void testEPLDataflowOpEventBusSink() {
        RegressionRunner.run(session, EPLDataflowOpEventBusSink.executions());
    }

    public void testEPLDataflowOpLogSink() {
        RegressionRunner.run(session, new EPLDataflowOpLogSink());
    }

    public void testEPLDataflowOpFilter() {
        RegressionRunner.run(session, EPLDataflowOpFilter.executions());
    }

    public void testEPLDataflowOpEventBusSource() {
        RegressionRunner.run(session, EPLDataflowOpEventBusSource.executions());
    }

    public void testEPLDataflowOpEPStatementSource() {
        RegressionRunner.run(session, EPLDataflowOpEPStatementSource.executions());
    }

    public void testEPLDataflowOpSelect() {
        RegressionRunner.run(session, EPLDataflowOpSelect.executions());
    }

    public void testEPLDataflowAPICreateStartStopDestroy() {
        RegressionRunner.run(session, EPLDataflowAPICreateStartStopDestroy.executions());
    }

    public void testEPLDataflowAPIExceptions() {
        RegressionRunner.run(session, new EPLDataflowAPIExceptions());
    }

    public void testEPLDataflowAPIInstantiationOptions() {
        RegressionRunner.run(session, EPLDataflowAPIInstantiationOptions.executions());
    }

    public void testEPLDataflowAPIOpLifecycle() {
        RegressionRunner.run(session, EPLDataflowAPIOpLifecycle.executions());
    }

    public void testEPLDataflowAPIRunStartCancelJoin() {
        RegressionRunner.run(session, EPLDataflowAPIRunStartCancelJoin.executions());
    }

    public void testEPLDataflowAPIStartCaptive() {
        RegressionRunner.run(session, new EPLDataflowAPIStartCaptive());
    }

    public void testEPLDataflowAPIStatistics() {
        RegressionRunner.run(session, new EPLDataflowAPIStatistics());
    }

    public void testEPLDataflowCustomProperties() {
        RegressionRunner.run(session, EPLDataflowCustomProperties.executions());
    }

    public void testEPLDataflowDocSamples() {
        RegressionRunner.run(session, EPLDataflowDocSamples.executions());
    }

    public void testEPLDataflowExampleRollingTopWords() {
        RegressionRunner.run(session, new EPLDataflowExampleRollingTopWords());
    }

    public void testEPLDataflowExampleVwapFilterSelectJoin() {
        RegressionRunner.run(session, new EPLDataflowExampleVwapFilterSelectJoin());
    }

    public void testEPLDataflowExampleWordCount() {
        RegressionRunner.run(session, new EPLDataflowExampleWordCount());
    }

    public void testEPLDataflowInputOutputVariations() {
        RegressionRunner.run(session, EPLDataflowInputOutputVariations.executions());
    }

    public void testEPLDataflowInvalidGraph() {
        RegressionRunner.run(session, EPLDataflowInvalidGraph.executions());
    }

    public void testEPLDataflowTypes() {
        RegressionRunner.run(session, EPLDataflowTypes.executions());
    }

    public void testEPLDataflowAPIConfigAndInstance() {
        RegressionRunner.run(session, new EPLDataflowAPIConfigAndInstance());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_A.class, SupportBean_B.class,
            SupportBean_S0.class, SupportBean_S1.class, SupportBean_S2.class}) {
            configuration.getCommon().addEventType(clazz.getSimpleName(), clazz);
        }

        configuration.getCommon().addEventType("MyOAEventType", "p0,p1".split(","), new Object[]{String.class, int.class});

        ConfigurationCommonEventTypeBean legacy = new ConfigurationCommonEventTypeBean();
        configuration.getCommon().addEventType("MyLegacyEvent", EPLDataflowOpBeaconSource.MyLegacyEvent.class.getName(), legacy);
        configuration.getCommon().addEventType("MyEventNoDefaultCtor", EPLDataflowOpBeaconSource.MyEventNoDefaultCtor.class);

        configuration.getCompiler().addPlugInSingleRowFunction("generateTagId", EPLDataflowOpBeaconSource.class.getName(), "generateTagId");

        DefaultSupportGraphEventUtil.addTypeConfiguration(configuration);

        configuration.getCommon().addImport(DefaultSupportSourceOp.class.getPackage().getName() + ".*");
        configuration.getCommon().addImport(DefaultSupportCaptureOp.class.getPackage().getName() + ".*");
        configuration.getCommon().addImport(EPLDataflowCustomProperties.MyOperatorOneForge.class);
        configuration.getCommon().addImport(EPLDataflowCustomProperties.MyOperatorTwoForge.class);
        configuration.getCommon().addImport(EPLDataflowCustomProperties.MyOperatorThreeForge.class);
        configuration.getCommon().addImport(EPLDataflowCustomProperties.MyOperatorFourForge.class);
        configuration.getCommon().addImport(EPLDataflowAPIOpLifecycle.SupportGraphSourceForge.class);
        configuration.getCommon().addImport(EPLDataflowAPIOpLifecycle.SupportOperatorForge.class);
        configuration.getCommon().addImport(EPLDataflowAPIExceptions.MyExceptionOpForge.class);
        configuration.getCommon().addImport(EPLDataflowAPIOpLifecycle.MyCaptureOutputPortOpForge.class);
        configuration.getCommon().addImport(EPLDataflowExampleRollingTopWords.class.getName());
        configuration.getCommon().addImport(EPLDataflowInputOutputVariations.MyFactorialOp.class);
        configuration.getCommon().addImport(EPLDataflowInputOutputVariations.MyCustomOp.class);
        configuration.getCommon().addImport(EPLDataflowInvalidGraph.MyInvalidOpForge.class);
        configuration.getCommon().addImport(EPLDataflowInvalidGraph.MyTestOp.class);
        configuration.getCommon().addImport(EPLDataflowInvalidGraph.MySBInputOp.class);
        configuration.getCommon().addImport(EPLDataflowTypes.MySupportBeanOutputOp.class);
        configuration.getCommon().addImport(EPLDataflowTypes.MyMapOutputOp.class);
        configuration.getCommon().addImport(MyLineFeedSource.class);
        configuration.getCommon().addImport(EPLDataflowAPIInstantiationOptions.MyOpForge.class);
        configuration.getCommon().addImport(MyObjectArrayGraphSource.class.getPackage().getName() + ".*");
        configuration.getCommon().addImport(MyTokenizerCounter.class.getPackage().getName() + ".*");
        configuration.getCommon().addImport(SupportBean.class);
    }
}
