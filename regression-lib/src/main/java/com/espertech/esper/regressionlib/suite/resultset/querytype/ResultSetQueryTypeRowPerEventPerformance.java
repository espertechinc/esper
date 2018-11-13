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
package com.espertech.esper.regressionlib.suite.resultset.querytype;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataIDBean;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;

import static org.junit.Assert.assertTrue;

public class ResultSetQueryTypeRowPerEventPerformance implements RegressionExecution {
    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public void run(RegressionEnvironment env) {
        EPStatement[] statements = new EPStatement[100];
        SupportUpdateListener[] listeners = new SupportUpdateListener[statements.length];
        for (int i = 0; i < statements.length; i++) {
            int secondsWindowSpan = i % 30 + 1;
            double percent = 0.25 + i;
            int id = i % 5;

            String text = "@name('s" + i + "') select symbol, min(price) " +
                "from SupportMarketDataBean(id='${id}')#time(${secondsWindowSpan})\n" +
                "having price >= min(price) * ${percent}";

            text = text.replace("${id}", Integer.toString(id));
            text = text.replace("${secondsWindowSpan}", Integer.toString(secondsWindowSpan));
            text = text.replace("${percent}", Double.toString(percent));

            statements[i] = env.compileDeploy(text).statement("s" + i);
            listeners[i] = new SupportUpdateListener();
            statements[i].addListener(listeners[i]);
        }

        long start = System.currentTimeMillis();
        int count = 0;
        for (int i = 0; i < 10000; i++) {
            count++;
            if (i % 10000 == 0) {
                long now = System.currentTimeMillis();
                double deltaSec = (now - start) / 1000.0;
                double throughput = 10000.0 / deltaSec;
                for (int j = 0; j < listeners.length; j++) {
                    listeners[j].reset();
                }
                start = now;
            }

            SupportMarketDataIDBean bean = new SupportMarketDataIDBean("IBM", Integer.toString(i % 5), 1);
            env.sendEventBean(bean);
        }
        long end = System.currentTimeMillis();
        long delta = end - start;
        assertTrue("Delta=" + delta, delta < 2000);
        //System.out.println("total=" + count + " delta=" + delta + " per sec:" + 10000.0 / (delta / 1000.0));

        env.undeployAll();
    }
}
