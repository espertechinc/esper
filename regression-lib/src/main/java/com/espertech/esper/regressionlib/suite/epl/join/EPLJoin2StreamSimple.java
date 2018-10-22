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

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EPLJoin2StreamSimple implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String text = "@name('s0') select irstream s0.price, s1.price from SupportMarketDataBean(symbol='S0')#length(3) as s0," +
            "SupportMarketDataBean(symbol='S1')#length(3) as s1 " +
            " where s0.volume = s1.volume";
        env.compileDeployAddListenerMileZero(text, "s0");

        env.sendEventBean(makeMarketDataEvent("S0", 100, 1));
        assertFalse(env.listener("s0").isInvoked());

        env.milestone(1);

        env.sendEventBean(makeMarketDataEvent("S1", 20, 1));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(),
            new String[]{"s0.price", "s1.price"},
            new Object[][]{{100.0, 20.0}});
        assertEquals(0, env.listener("s0").getOldDataListFlattened().length);
        env.listener("s0").reset();

        env.milestone(2);

        env.sendEventBean(makeMarketDataEvent("S1", 21, 1));
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getNewDataListFlattened(),
            new String[]{"s0.price", "s1.price"},
            new Object[][]{{100.0, 21.0}});
        assertEquals(0, env.listener("s0").getOldDataListFlattened().length);
        env.listener("s0").reset();

        env.milestone(3);

        env.sendEventBean(makeMarketDataEvent("S1", 22, 2));
        assertFalse(env.listener("s0").isInvoked());

        env.milestone(4);

        env.sendEventBean(makeMarketDataEvent("S1", 23, 3));
        assertEquals(0, env.listener("s0").getNewDataListFlattened().length);
        EPAssertionUtil.assertPropsPerRow(env.listener("s0").getOldDataListFlattened(),
            new String[]{"s0.price", "s1.price"},
            new Object[][]{{100.0, 20.0}});
        env.listener("s0").reset();

        env.milestone(5);

        env.undeployAll();
    }

    private static SupportMarketDataBean makeMarketDataEvent(String symbol, double price, long volume) {
        return new SupportMarketDataBean(symbol, price, volume, null);
    }
}

