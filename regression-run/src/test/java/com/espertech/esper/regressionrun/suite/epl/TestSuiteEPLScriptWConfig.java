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

import com.espertech.esper.regressionlib.suite.epl.script.EPLScriptExpressionConfiguration;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.suite.epl.script.EPLScriptExpressionDisable;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteEPLScriptWConfig extends TestCase {
    public void testEPLScriptExpressionConfiguration() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        session.getConfiguration().getCompiler().getScripts().setDefaultDialect("dummy");
        RegressionRunner.run(session, new EPLScriptExpressionConfiguration());
        session.destroy();
    }

    public void testEPLScriptExpressionDisable() {
        RegressionSession session = RegressionRunner.session();
        session.getConfiguration().getCommon().addEventType(SupportBean.class);
        session.getConfiguration().getCompiler().getScripts().setEnabled(false);
        RegressionRunner.run(session, new EPLScriptExpressionDisable());
        session.destroy();
    }
}
