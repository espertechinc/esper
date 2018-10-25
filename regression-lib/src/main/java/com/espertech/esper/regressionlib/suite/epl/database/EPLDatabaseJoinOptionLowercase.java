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

public class EPLDatabaseJoinOptionLowercase implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        String sql = "select myint from mytesttable where ${intPrimitive} = myint'" +
            "metadatasql 'select myint from mytesttable'";
        String stmtText = "@name('s0') select myint from " +
            " sql:MyDBLowerCase ['" + sql + "] as s0," +
            "SupportBean#length(100) as s1";
        env.compileDeploy(stmtText).addListener("s0");

        assertEquals(String.class, env.statement("s0").getEventType().getPropertyType("myint"));

        sendSupportBeanEvent(env, 10);
        assertEquals("10", env.listener("s0").assertOneGetNewAndReset().get("myint"));

        sendSupportBeanEvent(env, 80);
        assertEquals("80", env.listener("s0").assertOneGetNewAndReset().get("myint"));

        env.undeployAll();
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }
}
