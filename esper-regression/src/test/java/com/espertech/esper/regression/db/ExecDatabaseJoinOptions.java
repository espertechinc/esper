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
package com.espertech.esper.regression.db;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationDBRef;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.junit.Assert;

import static com.espertech.esper.regression.db.ExecDatabaseJoinOptionUppercase.getDBConfig;

public class ExecDatabaseJoinOptions implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationDBRef dbconfig = getDBConfig();
        configuration.addDatabaseReference("MyDB", dbconfig);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionNoMetaLexAnalysis(epService);
        runAssertionNoMetaLexAnalysisGroup(epService);
        runAssertionPlaceholderWhere(epService);
    }

    private void runAssertionNoMetaLexAnalysis(EPServiceProvider epService) {
        String sql = "select mydouble from mytesttable where ${intPrimitive} = myint";
        run(epService, sql);
    }

    private void runAssertionNoMetaLexAnalysisGroup(EPServiceProvider epService) {
        String sql = "select mydouble, sum(myint) from mytesttable where ${intPrimitive} = myint group by mydouble";
        run(epService, sql);
    }

    private void runAssertionPlaceholderWhere(EPServiceProvider epService) {
        String sql = "select mydouble from mytesttable ${$ESPER-SAMPLE-WHERE} where ${intPrimitive} = myint";
        run(epService, sql);
    }

    private void run(EPServiceProvider epService, String sql) {
        String stmtText = "select mydouble from " +
                " sql:MyDB ['" + sql + "'] as s0," +
                SupportBean.class.getName() + "#length(100) as s1";

        EPStatement statement = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        statement.addListener(listener);

        Assert.assertEquals(Double.class, statement.getEventType().getPropertyType("mydouble"));

        sendSupportBeanEvent(epService, 10);
        Assert.assertEquals(1.2, listener.assertOneGetNewAndReset().get("mydouble"));

        sendSupportBeanEvent(epService, 80);
        Assert.assertEquals(8.2, listener.assertOneGetNewAndReset().get("mydouble"));

        statement.destroy();
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }
}
