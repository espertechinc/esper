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
package com.espertech.esper.regressionlib.suite.epl.database;

import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertEquals;

public class EPLDatabaseJoinOptionUppercase implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String sql = "select myint from mytesttable where ${theString} = myvarchar'" +
            "metadatasql 'select myint from mytesttable'";
        String stmtText = "@name('s0') select MYINT from " +
            " sql:MyDBUpperCase ['" + sql + "] as s0," +
            "SupportBean#length(100) as s1";
        env.compileDeploy(stmtText).addListener("s0");

        env.assertStatement("s0", statement -> assertEquals(Integer.class, statement.getEventType().getPropertyType("MYINT")));

        sendSupportBeanEvent(env, "A");
        env.assertEqualsNew("s0", "MYINT", 10);

        sendSupportBeanEvent(env, "H");
        env.assertEqualsNew("s0", "MYINT", 80);

        env.undeployAll();
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        env.sendEventBean(bean);
    }
}
