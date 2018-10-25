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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class EPLDatabaseDataSourceFactory implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(EPLDatabaseDataSourceFactory.class);

    public void run(RegressionEnvironment env) {
        String[] fields = new String[]{"myint"};
        String stmtText = "@name('s0') select istream myint from " +
            " sql:MyDBWithPooledWithLRU100 ['select myint from mytesttable where ${intPrimitive} = mytesttable.mybigint'] as s0," +
            "SupportBean as s1";
        env.compileDeploy(stmtText).addListener("s0");

        sendSupportBeanEvent(env, 10);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{100});

        sendSupportBeanEvent(env, 6);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{60});

        long startTime = System.currentTimeMillis();
        // Send 100 events which all fireStatementStopped a join
        for (int i = 0; i < 100; i++) {
            sendSupportBeanEvent(env, 10);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), fields, new Object[]{100});
        }
        long endTime = System.currentTimeMillis();
        log.info("delta=" + (endTime - startTime));
        assertTrue(endTime - startTime < 5000);

        env.undeployAll();
    }

    private static void sendSupportBeanEvent(RegressionEnvironment env, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
    }
}
