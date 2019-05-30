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
package com.espertech.esper.regressionrun.suite.client;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.regressionlib.suite.client.compile.*;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteClientCompile extends TestCase {

    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testClientCompileOutput() {
        RegressionRunner.run(session, ClientCompileOutput.executions());
    }

    public void testClientCompileVisibility() {
        RegressionRunner.run(session, ClientCompileVisibility.executions());
    }

    public void testClientCompileSPI() {
        RegressionRunner.run(session, ClientCompileSPI.executions());
    }

    public void testClientCompileUserObject() {
        RegressionRunner.run(session, ClientCompileUserObject.executions());
    }

    public void testClientCompileStatementName() {
        RegressionRunner.run(session, ClientCompileStatementName.executions());
    }

    public void testClientCompileModule() {
        RegressionRunner.run(session, ClientCompileModule.executions());
    }

    public void testClientCompileSyntaxValidate() {
        RegressionRunner.run(session, ClientCompileSyntaxValidate.executions());
    }

    public void testClientCompileModuleUses() {
        RegressionRunner.run(session, ClientCompileModuleUses.executions());
    }

    public void testClientCompileStatementObjectModel() {
        RegressionRunner.run(session, ClientCompileStatementObjectModel.executions());
    }

    public void testClientCompileSubstitutionParams() {
        RegressionRunner.run(session, ClientCompileSubstitutionParams.executions());
    }

    public void testClientCompileEnginePath() {
        RegressionRunner.run(session, ClientCompileEnginePath.executions());
    }

    public void testClientCompileEventTypeAutoName() {
        RegressionRunner.run(session, ClientCompileEventTypeAutoName.executions());
    }

    public void testClientCompileExceptionItems() {
        RegressionRunner.run(session, ClientCompileExceptionItems.executions());
    }

    public void testClientCompileLarge() {
        RegressionRunner.run(session, ClientCompileLarge.executions());
    }

    private static void configure(Configuration configuration) {

        for (Class clazz : new Class[]{SupportBean.class, SupportMarketDataBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            configuration.getCommon().addEventType(clazz.getSimpleName(), clazz);
        }

        configuration.getCommon().addVariable("preconfigured_variable", int.class, 5, true);

        configuration.getCompiler().getByteCode().setAttachModuleEPL(true);
        configuration.getCommon().addImport(SupportBean.class);
        configuration.getCommon().addImport(ClientCompileSubstitutionParams.IKey.class);
        configuration.getCommon().addImport(ClientCompileSubstitutionParams.MyObjectKeyConcrete.class);

        configuration.getCommon().addEventTypeAutoName("com.espertech.esper.regressionlib.support.autoname.one");
        configuration.getCommon().addEventTypeAutoName("com.espertech.esper.regressionlib.support.autoname.two");

        configuration.getCompiler().addPlugInSingleRowFunction("func", ClientCompileLarge.class.getName(), "func");
    }
}
