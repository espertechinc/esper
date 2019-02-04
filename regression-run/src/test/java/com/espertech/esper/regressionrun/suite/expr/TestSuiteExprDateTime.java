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
import com.espertech.esper.common.client.configuration.common.ConfigurationCommon;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeBean;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeObjectArray;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.suite.expr.datetime.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.schedule.SupportDateTimeFieldType;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TestSuiteExprDateTime extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testExprDTBetween() {
        RegressionRunner.run(session, ExprDTBetween.executions());
    }

    public void testExprDTDocSamples() {
        RegressionRunner.run(session, new ExprDTDocSamples());
    }

    public void testExprDTFormat() {
        RegressionRunner.run(session, ExprDTFormat.executions());
    }

    public void testExprDTGet() {
        RegressionRunner.run(session, ExprDTGet.executions());
    }

    public void testExprDTIntervalOps() {
        RegressionRunner.run(session, ExprDTIntervalOps.executions());
    }

    public void testExprDTIntervalOpsCreateSchema() {
        RegressionRunner.run(session, new ExprDTIntervalOpsCreateSchema());
    }

    public void testExprDTInvalid() {
        RegressionRunner.run(session, new ExprDTInvalid());
    }

    public void testExprDTResolution() {
        RegressionRunner.run(session, ExprDTResolution.executions(false));
    }

    public void testExprDTNested() {
        RegressionRunner.run(session, new ExprDTNested());
    }

    public void testExprDTPerfBetween() {
        RegressionRunner.run(session, new ExprDTPerfBetween());
    }

    public void testExprDTPerfIntervalOps() {
        RegressionRunner.run(session, new ExprDTPerfIntervalOps());
    }

    public void testExprDTPlusMinus() {
        RegressionRunner.run(session, ExprDTPlusMinus.executions());
    }

    public void testExprDTDataSources() {
        RegressionRunner.run(session, ExprDTDataSources.executions());
    }

    public void testExprDTRound() {
        RegressionRunner.run(session, ExprDTRound.executions());
    }

    public void testExprDTSet() {
        RegressionRunner.run(session, ExprDTSet.executions());
    }

    public void testExprDTToDateCalMSec() {
        RegressionRunner.run(session, ExprDTToDateCalMSec.executions());
    }

    public void testExprDTWithDate() {
        RegressionRunner.run(session, new ExprDTWithDate());
    }

    public void testExprDTWithMax() {
        RegressionRunner.run(session, ExprDTWithMax.executions());
    }

    public void testExprDTWithMin() {
        RegressionRunner.run(session, ExprDTWithMin.executions());
    }

    public void testExprDTWithTime() {
        RegressionRunner.run(session, new ExprDTWithTime());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportDateTime.class, SupportTimeStartEndA.class, SupportBean.class, SupportEventWithJustGet.class,
            SupportBean_ST0_Container.class
        }) {
            configuration.getCommon().addEventType(clazz);
        }

        Map<String, Object> meta = new HashMap<>();
        meta.put("timeTaken", Date.class);
        configuration.getCommon().addEventType("RFIDEvent", meta);

        ConfigurationCommon common = configuration.getCommon();
        common.addVariable("V_START", long.class, -1);
        common.addVariable("V_END", long.class, -1);

        ConfigurationCommonEventTypeBean leg = new ConfigurationCommonEventTypeBean();
        leg.setStartTimestampPropertyName("longdateStart");
        configuration.getCommon().addEventType("A", SupportTimeStartEndA.class.getName(), leg);
        configuration.getCommon().addEventType("B", SupportTimeStartEndB.class.getName(), leg);

        ConfigurationCommonEventTypeBean configBean = new ConfigurationCommonEventTypeBean();
        configBean.setStartTimestampPropertyName("longdateStart");
        configBean.setEndTimestampPropertyName("longdateEnd");
        configuration.getCommon().addEventType("SupportTimeStartEndA", SupportTimeStartEndA.class.getName(), configBean);
        configuration.getCommon().addEventType("SupportTimeStartEndB", SupportTimeStartEndB.class.getName(), configBean);

        configuration.getCommon().addImport(DateTime.class);
        configuration.getCommon().addImport(SupportBean_ST0_Container.class);
        configuration.getCompiler().addPlugInSingleRowFunction("makeTest", SupportBean_ST0_Container.class.getName(), "makeTest");

        for (SupportDateTimeFieldType fieldType : SupportDateTimeFieldType.values()) {
            ConfigurationCommonEventTypeObjectArray oa = new ConfigurationCommonEventTypeObjectArray();
            oa.setStartTimestampPropertyName("startTS");
            oa.setEndTimestampPropertyName("endTS");
            configuration.getCommon().addEventType("A_" + fieldType.name(), "startTS,endTS".split(","), new Object[]{fieldType.getClazz(), fieldType.getClazz()}, oa);
            configuration.getCommon().addEventType("B_" + fieldType.name(), "startTS,endTS".split(","), new Object[]{fieldType.getClazz(), fieldType.getClazz()}, oa);
        }

        addIdStsEtsEvent(configuration);
    }

    static void addIdStsEtsEvent(Configuration configuration) {
        ConfigurationCommonEventTypeObjectArray oa = new ConfigurationCommonEventTypeObjectArray();
        oa.setStartTimestampPropertyName("sts");
        oa.setEndTimestampPropertyName("ets");
        configuration.getCommon().addEventType("MyEvent", "id,sts,ets".split(","), new Object[]{String.class, long.class, long.class}, oa);
    }
}
