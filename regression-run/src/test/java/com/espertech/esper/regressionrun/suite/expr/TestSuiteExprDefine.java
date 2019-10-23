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
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.regressionlib.suite.expr.define.*;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.lrreport.LRUtil;
import com.espertech.esper.regressionlib.support.lrreport.LocationReport;
import com.espertech.esper.regressionrun.runner.RegressionRunner;
import com.espertech.esper.regressionrun.runner.RegressionSession;
import junit.framework.TestCase;

public class TestSuiteExprDefine extends TestCase {
    private RegressionSession session;

    public void setUp() {
        session = RegressionRunner.session();
        configure(session.getConfiguration());
    }

    public void tearDown() {
        session.destroy();
        session = null;
    }

    public void testExprDefineBasic() {
        RegressionRunner.run(session, ExprDefineBasic.executions());
    }

    public void testExprDefineAliasFor() {
        RegressionRunner.run(session, ExprDefineAliasFor.executions());
    }

    public void testExprDefineLambdaLocReport() {
        RegressionRunner.run(session, new ExprDefineLambdaLocReport());
    }

    public void testExprDefineValueParameter() {
        RegressionRunner.run(session, ExprDefineValueParameter.executions());
    }

    public void testExprDefineEventParameterNonStream() {
        RegressionRunner.run(session, ExprDefineEventParameterNonStream.executions());
    }

    private static void configure(Configuration configuration) {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class,
            SupportBean_ST0.class, SupportBean_ST1.class, SupportBean_ST0_Container.class, SupportCollection.class,
            SupportBeanObject.class, LocationReport.class}) {
            configuration.getCommon().addEventType(clazz);
        }

        configuration.getCommon().addImport(LRUtil.class);
        configuration.getCommon().addImport(ExprDefineValueParameter.ExprDefineLocalService.class);
    }
}
