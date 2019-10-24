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
package com.espertech.esper.regressionrun.suite.context;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonDBRef;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInSingleRowFunction;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.regressionlib.suite.context.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.extend.vdw.SupportVirtualDW;
import com.espertech.esper.regressionlib.support.extend.vdw.SupportVirtualDWForge;
import com.espertech.esper.regressionlib.support.util.SupportDatabaseService;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.Properties;

public class TestSuiteContext extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testContextCategory() {
        RegressionRunner.run(session, ContextCategory.executions());
    }

    public void testContextKeySegmented() {
        RegressionRunner.run(session, ContextKeySegmented.executions());
    }

    public void testContextKeySegmentedAggregate() {
        RegressionRunner.run(session, ContextKeySegmentedAggregate.executions());
    }

    public void testContextHashSegmented() {
        RegressionRunner.run(session, ContextHashSegmented.executions());
    }

    public void testContextInitTerm() {
        RegressionRunner.run(session, ContextInitTerm.executions());
    }

    public void testContextInitTermTemporalFixed() {
        RegressionRunner.run(session, ContextInitTermTemporalFixed.executions());
    }

    public void testContextInitTermWithDistinct() {
        RegressionRunner.run(session, ContextInitTermWithDistinct.executions());
    }

    public void testContextInitTermWithNow() {
        RegressionRunner.run(session, ContextInitTermWithNow.executions());
    }

    public void testContextNested() {
        RegressionRunner.run(session, ContextNested.executions());
    }

    public void testContextAdminListen() {
        RegressionRunner.run(session, ContextAdminListen.executions());
    }

    public void testContextDocExamples() {
        RegressionRunner.run(session, new ContextDocExamples());
    }

    public void testContextKeySegmentedInfra() {
        RegressionRunner.run(session, ContextKeySegmentedInfra.executions());
    }

    public void testContextKeySegmentedNamedWindow() {
        RegressionRunner.run(session, ContextKeySegmentedNamedWindow.executions());
    }

    public void testContextLifecycle() {
        RegressionRunner.run(session, ContextLifecycle.executions());
    }

    public void testContextWDeclaredExpression() {
        RegressionRunner.run(session, ContextWDeclaredExpression.executions());
    }

    public void testContextVariables() {
        RegressionRunner.run(session, ContextVariables.executions());
    }

    public void testContextSelectionAndFireAndForget() {
        RegressionRunner.run(session, ContextSelectionAndFireAndForget.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class,
            SupportBean_S2.class, SupportBean_S3.class, ISupportBaseAB.class, ISupportA.class, SupportWebEvent.class,
            ISupportAImpl.class, SupportGroupSubgroupEvent.class, SupportEventWithIntArray.class}) {
            configuration.getCommon().addEventType(clazz.getSimpleName(), clazz);
        }

        configuration.getCommon().addEventType(ContextDocExamples.BankTxn.class);
        configuration.getCommon().addEventType(ContextDocExamples.LoginEvent.class);
        configuration.getCommon().addEventType(ContextDocExamples.LogoutEvent.class);
        configuration.getCommon().addEventType(ContextDocExamples.SecurityEvent.class);
        configuration.getCommon().addEventType(ContextDocExamples.SensorEvent.class);
        configuration.getCommon().addEventType(ContextDocExamples.TrafficEvent.class);
        configuration.getCommon().addEventType(ContextDocExamples.TrainEnterEvent.class);
        configuration.getCommon().addEventType(ContextDocExamples.TrainLeaveEvent.class);
        configuration.getCommon().addEventType(ContextDocExamples.CumulativePrice.class);
        configuration.getCommon().addEventType(ContextDocExamples.PassengerScanEvent.class);
        configuration.getCommon().addEventType(ContextDocExamples.MyStartEvent.class);
        configuration.getCommon().addEventType(ContextDocExamples.MyEndEvent.class);
        configuration.getCommon().addEventType(ContextDocExamples.MyInitEvent.class);
        configuration.getCommon().addEventType(ContextDocExamples.MyTermEvent.class);
        configuration.getCommon().addEventType(ContextDocExamples.MyEvent.class);
        configuration.getCommon().addEventType("StartEventOne", ContextDocExamples.MyStartEvent.class);
        configuration.getCommon().addEventType("StartEventTwo", ContextDocExamples.MyStartEvent.class);
        configuration.getCommon().addEventType("MyOtherEvent", ContextDocExamples.MyStartEvent.class);
        configuration.getCommon().addEventType("EndEventOne", ContextDocExamples.MyEndEvent.class);
        configuration.getCommon().addEventType("EndEventTwo", ContextDocExamples.MyEndEvent.class);
        configuration.getCommon().addEventType(ContextDocExamples.MyTwoKeyInit.class);
        configuration.getCommon().addEventType(ContextDocExamples.MyTwoKeyTerm.class);

        configuration.getCompiler().addPlugInSingleRowFunction("myHash", ContextHashSegmented.class.getName(), "myHashFunc");
        configuration.getCompiler().addPlugInSingleRowFunction("mySecond", ContextHashSegmented.class.getName(), "mySecondFunc");
        configuration.getCompiler().addPlugInSingleRowFunction("makeBean", ContextInitTermTemporalFixed.class.getName(), "singleRowPluginMakeBean");
        configuration.getCompiler().addPlugInSingleRowFunction("toArray", ContextKeySegmentedAggregate.class.getName(), "toArray");

        configuration.getCompiler().addPlugInSingleRowFunction(
            "customEnabled", ContextNested.class.getName(), "customMatch", ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.ENABLED);
        configuration.getCompiler().addPlugInSingleRowFunction(
            "customDisabled", ContextNested.class.getName(), "customMatch", ConfigurationCompilerPlugInSingleRowFunction.FilterOptimizable.DISABLED);
        configuration.getCompiler().addPlugInSingleRowFunction("stringContainsX", ContextKeySegmented.class.getName(), "stringContainsX");

        configuration.getCommon().addImport(ContextHashSegmented.class.getName());

        ConfigurationCommonDBRef configDB = new ConfigurationCommonDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configuration.getCommon().addDatabaseReference("MyDB", configDB);

        configuration.getCompiler().getByteCode().setAllowSubscriber(true);
        configuration.getCompiler().addPlugInVirtualDataWindow("test", "vdw", SupportVirtualDWForge.class.getName(), SupportVirtualDW.ITERATE);    // configure with iteration
    }
}
