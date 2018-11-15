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
package com.espertech.esper.regressionlib.suite.multithread;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import org.junit.Assert;

import java.util.LinkedList;
import java.util.List;

import static com.espertech.esper.regressionlib.support.client.SupportCompileDeployUtil.threadJoin;

public class MultithreadUpdateIStreamSubselect implements RegressionExecution {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        env.compileDeploy("@name('s0') update istream SupportBean as sb set longPrimitive = (select count(*) from SupportBean_S0#keepall as s0 where s0.p00 = sb.theString)");
        SupportUpdateListener listener = new SupportUpdateListener();
        env.statement("s0").addListener(listener);

        // insert 5 data events for each symbol
        int numGroups = 20;
        int numRepeats = 5;
        for (int i = 0; i < numGroups; i++) {
            for (int j = 0; j < numRepeats; j++) {
                env.sendEventBean(new SupportBean_S0(i, "S0_" + i)); // S0_0 .. S0_19 each has 5 events
            }
        }

        List<Thread> threads = new LinkedList<Thread>();
        for (int i = 0; i < numGroups; i++) {
            final int group = i;
            final Thread t = new Thread(new Runnable() {
                public void run() {
                    env.sendEventBean(new SupportBean("S0_" + group, 1));
                }
            }, MultithreadUpdateIStreamSubselect.class.getSimpleName());
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            threadJoin(t);
        }

        // validate results, price must be 5 for each symbol
        Assert.assertEquals(numGroups, listener.getNewDataList().size());
        for (EventBean[] newData : listener.getNewDataList()) {
            SupportBean result = (SupportBean) (newData[0]).getUnderlying();
            Assert.assertEquals(numRepeats, result.getLongPrimitive());
        }

        env.undeployAll();
    }
}

