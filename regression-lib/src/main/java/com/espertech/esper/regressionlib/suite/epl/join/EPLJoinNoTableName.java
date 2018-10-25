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
package com.espertech.esper.regressionlib.suite.epl.join;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import static org.junit.Assert.assertNotNull;

public class EPLJoinNoTableName implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String joinStatement = "@name('s0') select * from " +
            "SupportMarketDataBean#length(3)," +
            "SupportBean#length(3)" +
            " where symbol=theString and volume=longBoxed";
        env.compileDeploy(joinStatement).addListener("s0");

        Object[] setOne = new Object[5];
        Object[] setTwo = new Object[5];

        for (int i = 0; i < setOne.length; i++) {
            setOne[i] = new SupportMarketDataBean("IBM", 0, (long) i, "");

            SupportBean theEvent = new SupportBean();
            theEvent.setTheString("IBM");
            theEvent.setLongBoxed((long) i);
            setTwo[i] = theEvent;
        }

        sendEvent(env, setOne[0]);
        sendEvent(env, setTwo[0]);
        assertNotNull(env.listener("s0").getLastNewData());

        env.undeployAll();
    }

    private static void sendEvent(RegressionEnvironment env, Object theEvent) {
        env.sendEventBean(theEvent);
    }
}
