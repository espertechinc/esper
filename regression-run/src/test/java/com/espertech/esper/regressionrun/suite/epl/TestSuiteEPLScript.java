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
import com.espertech.esper.regressionlib.suite.epl.script.EPLScriptExpression;
import com.espertech.esper.regressionlib.suite.epl.script.EPLScriptSandboxJSR223;
import com.espertech.esper.regressionlib.suite.epl.script.EPLScriptSandboxMVEL;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.regressionlib.support.bean.SupportColorEvent;
import com.espertech.esper.regressionlib.support.bean.SupportRFIDSimpleEvent;
import com.espertech.esper.regressionlib.support.script.MyImportedClass;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteEPLScript extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testEPLScriptExpression() {
        RegressionRunner.run(session, EPLScriptExpression.executions());
    }

    public void testEPLScriptSandboxJSR223() {
        RegressionRunner.run(session, new EPLScriptSandboxJSR223());
    }

    public void testEPLScriptSandboxMVEL() {
        RegressionRunner.run(session, new EPLScriptSandboxMVEL());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportColorEvent.class, SupportRFIDSimpleEvent.class}) {
            configuration.getCommon().addEventType(clazz);
        }
        configuration.getCommon().addImport(MyImportedClass.class);
    }
}
