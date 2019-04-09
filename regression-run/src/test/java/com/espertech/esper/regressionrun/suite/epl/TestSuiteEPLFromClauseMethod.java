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
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInSingleRowFunction;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.suite.epl.fromclausemethod.EPLFromClauseMethod;
import com.espertech.esper.regressionlib.suite.epl.fromclausemethod.EPLFromClauseMethodNStream;
import com.espertech.esper.regressionlib.suite.epl.fromclausemethod.EPLFromClauseMethodOuterNStream;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportJoinMethods;
import com.espertech.esper.regressionlib.support.epl.SupportMethodInvocationJoinInvalid;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteEPLFromClauseMethod extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEPLFromClauseMethod() {
        RegressionRunner.run(session, EPLFromClauseMethod.executions());
    }

    public void testEPLFromClauseMethodNStream() {
        RegressionRunner.run(session, EPLFromClauseMethodNStream.executions());
    }

    public void testEPLFromClauseMethodOuterNStream() {
        RegressionRunner.run(session, EPLFromClauseMethodOuterNStream.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBeanTwo.class, SupportBean_A.class,
            SupportBean_S0.class, SupportBeanInt.class, SupportTradeEventWithSide.class,
            SupportEventWithManyArray.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        configuration.getCommon().getLogging().setEnableQueryPlan(true);

        ConfigurationCommon common = configuration.getCommon();
        common.addVariable("var1", Integer.class, 0);
        common.addVariable("var2", Integer.class, 0);
        common.addVariable("var3", Integer.class, 0);
        common.addVariable("var4", Integer.class, 0);
        common.addVariable("varN1", Integer.class, 0);
        common.addVariable("varN2", Integer.class, 0);
        common.addVariable("varN3", Integer.class, 0);
        common.addVariable("varN4", Integer.class, 0);

        configuration.getCommon().addImport(SupportJoinMethods.class.getName());
        configuration.getCommon().addImport(SupportMethodInvocationJoinInvalid.class);

        ConfigurationCompilerPlugInSingleRowFunction entry = new ConfigurationCompilerPlugInSingleRowFunction();
        entry.setName("myItemProducerUDF");
        entry.setFunctionClassName(EPLFromClauseMethod.class.getName());
        entry.setFunctionMethodName("myItemProducerUDF");
        entry.setEventTypeName("ItemEvent");
        configuration.getCompiler().addPlugInSingleRowFunction(entry);
    }
}
