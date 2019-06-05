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
import com.espertech.esper.common.client.configuration.common.ConfigurationCommon;
import com.espertech.esper.common.internal.support.*;
import com.espertech.esper.regressionlib.suite.epl.variable.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteEPLVariable extends TestCase {

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEPLVariables() {
        RegressionRunner.run(session, EPLVariables.executions());
    }

    public void testEPLVariablesCreate() {
        RegressionRunner.run(session, EPLVariablesCreate.executions());
    }

    public void testEPLVariablesDestroy() {
        RegressionRunner.run(session, EPLVariablesDestroy.executions());
    }

    public void testEPLVariablesEventTyped() {
        RegressionRunner.run(session, EPLVariablesEventTyped.executions());
    }

    public void testEPLVariablesPerf() {
        RegressionRunner.run(session, new EPLVariablesPerf());
    }

    public void testEPLVariablesOutputRate() {
        RegressionRunner.run(session, EPLVariablesOutputRate.executions());
    }

    private RegressionSession session;

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class, SupportBean_S2.class,
            SupportBean_A.class, SupportBean_B.class, SupportMarketDataBean.class, EPLVariables.MyVariableCustomEvent.class,
            SupportEventWithIntArray.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        configuration.getCommon().addImport(EPLVariables.MySimpleVariableServiceFactory.class);
        configuration.getCommon().addImport(EPLVariables.MySimpleVariableService.class);
        configuration.getCommon().addImport(EPLVariables.MyVariableCustomType.class);

        configuration.getCommon().addImport(SupportEnum.class);

        ConfigurationCommon common = configuration.getCommon();
        common.addVariable("var_simple_preconfig_const", "boolean", true, true);
        common.addVariable("MYCONST_THREE", "boolean", true, true);
        common.addVariable("papi_1", String.class, "begin");
        common.addVariable("papi_2", boolean.class, true);
        common.addVariable("papi_3", String.class, "value");
        common.addVariable("myRuntimeInitService", EPLVariables.MySimpleVariableService.class, null);
        common.addVariable("MYCONST_TWO", "string", null, true);
        common.addVariable("varcoll", "String[]", new String[]{"E1", "E2"}, true);
        common.addVariable("mySimpleVariableService", EPLVariables.MySimpleVariableService.class, null);
        common.addVariable("myInitService", EPLVariables.MySimpleVariableService.class, EPLVariables.MySimpleVariableServiceFactory.makeService());
        common.addVariable("supportEnum", SupportEnum.class.getName(), SupportEnum.ENUM_VALUE_1);
        common.addVariable("enumWithOverride", EPLVariables.MyEnumWithOverride.class.getName(), EPLVariables.MyEnumWithOverride.LONG);
        common.addVariable("var1", int.class, -1);
        common.addVariable("var2", String.class, "abc");
        common.addVariable("var1SS", String.class, "a");
        common.addVariable("var2SS", String.class, "b");
        common.addVariable("var1IFB", String.class, null);
        common.addVariable("var2IFB", String.class, null);
        common.addVariable("var1IF", String.class, null);
        common.addVariable("var1OND", Integer.class, "12");
        common.addVariable("var2OND", Integer.class, "2");
        common.addVariable("var3OND", Integer.class, null);
        common.addVariable("var1OD", Integer.class, 0);
        common.addVariable("var2OD", Integer.class, 1);
        common.addVariable("var3OD", Integer.class, 2);
        common.addVariable("var1OM", double.class, 10d);
        common.addVariable("var2OM", Long.class, 11L);
        common.addVariable("var1C", double.class, 10d);
        common.addVariable("var2C", Long.class, 11L);
        common.addVariable("var1RTC", Integer.class, 10);
        common.addVariable("var1ROM", Integer.class, null);
        common.addVariable("var2ROM", Integer.class, 1);
        common.addVariable("var1COE", Float.class, null);
        common.addVariable("var2COE", Double.class, null);
        common.addVariable("var3COE", Long.class, null);
        common.addVariable("var1IS", String.class, null);
        common.addVariable("var2IS", boolean.class, false);
        common.addVariable("var3IS", int.class, 1);
        common.addVariable("MyPermanentVar", String.class, "thevalue");
        common.addVariable("vars0_A", "SupportBean_S0", new SupportBean_S0(10));
        common.addVariable("vars1_A", SupportBean_S1.class.getName(), new SupportBean_S1(20));
        common.addVariable("varsobj1", Object.class.getName(), 123, true);
        common.addVariable("vars2", "SupportBean_S2", new SupportBean_S2(30));
        common.addVariable("vars3", SupportBean_S3.class, new SupportBean_S3(40));
        common.addVariable("varsobj2", Object.class, "ABC", true);
        common.addVariable("var_output_limit", long.class, "3");
        common.addVariable("myNonSerializable", EPLVariablesEventTyped.NonSerializable.class, EPLVariablesEventTyped.NON_SERIALIZABLE);
        common.addVariable("my_variable_custom_typed", EPLVariables.MyVariableCustomType.class.getName(), EPLVariables.MyVariableCustomType.of("abc"), true);
        common.addVariable("varargsTestClient", EPLVariables.SupportVarargsClient.class, new EPLVariables.SupportVarargsClientImpl());

        configuration.getCompiler().getViewResources().setIterableUnbound(true);
        configuration.getCompiler().getByteCode().setAllowSubscriber(true);
    }
}
