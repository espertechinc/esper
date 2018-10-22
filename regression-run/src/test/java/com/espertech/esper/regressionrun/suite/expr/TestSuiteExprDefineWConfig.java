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
import com.espertech.esper.regressionlib.suite.expr.define.ExprDefineConfigurations;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST0;
import com.espertech.esper.regressionlib.support.bean.SupportBean_ST1;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteExprDefineWConfig extends TestCase {
    public void testExprDefineConfigurations() {
        run(null, new ExprDefineConfigurations(4));
        run(0, new ExprDefineConfigurations(4));
        run(1, new ExprDefineConfigurations(4));
        run(2, new ExprDefineConfigurations(2));
    }

    private static void run(Integer configuredCacheSize, ExprDefineConfigurations exec) {
        RegressionSession session = RegressionRunner.session();

        Configuration configuration = session.getConfiguration();
        if (configuredCacheSize != null) {
            configuration.getRuntime().getExecution().setDeclaredExprValueCacheSize(configuredCacheSize);
        }
        for (Class clazz : new Class[]{SupportBean_ST0.class, SupportBean_ST1.class}) {
            configuration.getCommon().addEventType(clazz);
        }
        configuration.getCompiler().addPlugInSingleRowFunction("alwaysTrue", SupportStaticMethodLib.class.getName(), "alwaysTrue");

        RegressionRunner.run(session, exec);
        session.destroy();
    }
}
