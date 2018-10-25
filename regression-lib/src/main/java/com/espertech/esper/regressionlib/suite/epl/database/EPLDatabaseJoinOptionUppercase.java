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

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;

import static com.espertech.esper.common.client.scopetest.ScopeTestHelper.assertEquals;

public class EPLDatabaseJoinOptionUppercase implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String sql = "select myint from mytesttable where ${theString} = myvarchar'" +
            "metadatasql 'select myint from mytesttable'";
        String stmtText = "@name('s0') select MYINT from " +
            " sql:MyDBUpperCase ['" + sql + "] as s0," +
            "SupportBean#length(100) as s1";
        env.compileDeploy(stmtText).addListener("s0");

        assertEquals(Integer.class, env.statement("s0").getEventType().getPropertyType("MYINT"));

        sendSupportBeanEvent(env, "A");
        assertEquals(10, env.listener("s0").assertOneGetNewAndReset().get("MYINT"));

        sendSupportBeanEvent(env, "H");
        assertEquals(80, env.listener("s0").assertOneGetNewAndReset().get("MYINT"));

        env.undeployAll();
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        env.sendEventBean(bean);
    }
}
