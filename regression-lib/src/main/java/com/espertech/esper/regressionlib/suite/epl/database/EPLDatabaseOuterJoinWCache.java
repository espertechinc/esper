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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EPLDatabaseOuterJoinWCache implements RegressionExecution {

    public void run(RegressionEnvironment env) {

        String stmtText = "@name('s0') select * from SupportBean as sb " +
            "left outer join " +
            "sql:MyDBWithExpiryTime ['select myint from mytesttable'] as t " +
            "on sb.intPrimitive = t.myint " +
            "where myint is null";
        env.compileDeploy(stmtText).addListener("s0");

        env.sendEventBean(new SupportBean("E1", -1));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("E2", 10));
        assertFalse(env.listener("s0").getAndClearIsInvoked());

        env.sendEventBean(new SupportBean("E1", 1));
        assertTrue(env.listener("s0").getAndClearIsInvoked());

        env.undeployAll();
    }
}
