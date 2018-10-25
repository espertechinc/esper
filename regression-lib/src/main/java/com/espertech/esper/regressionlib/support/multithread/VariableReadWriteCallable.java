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
package com.espertech.esper.regressionlib.support.multithread;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;
import junit.framework.AssertionFailedError;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class VariableReadWriteCallable implements Callable {
    private final EPRuntime runtime;
    private final int numRepeats;
    private final int threadNum;
    private final SupportUpdateListener selectListener;

    public VariableReadWriteCallable(int threadNum, RegressionEnvironment env, int numRepeats) {
        this.runtime = env.runtime();
        this.numRepeats = numRepeats;
        this.threadNum = threadNum;

        selectListener = new SupportUpdateListener();
        String stmtText = "@name('t" + threadNum + "') select var1, var2, var3 from SupportBean_A(id='" + threadNum + "')";
        env.compileDeploy(stmtText).statement("t" + threadNum).addListener(selectListener);
    }

    public Object call() throws Exception {
        try {
            for (int loop = 0; loop < numRepeats; loop++) {
                long newValue = threadNum * 1000000 + loop;
                Object theEvent;

                if (loop % 2 == 0) {
                    theEvent = new SupportMarketDataBean("", 0, newValue, "");
                } else {
                    SupportBean bean = new SupportBean();
                    bean.setLongPrimitive(newValue);
                    theEvent = bean;
                }

                // Changes the variable values through either of the set-statements
                runtime.getEventService().sendEventBean(theEvent, theEvent.getClass().getSimpleName());

                // Select the variable value back, another thread may have changed it, we are only
                // determining if the set operation is atomic
                runtime.getEventService().sendEventBean(new SupportBean_A(Integer.toString(threadNum)), SupportBean_A.class.getSimpleName());
                EventBean received = selectListener.assertOneGetNewAndReset();
                Assert.assertEquals(received.get("var1"), received.get("var2"));
            }
        } catch (AssertionFailedError ex) {
            log.error("Assertion error in thread " + Thread.currentThread().getId(), ex);
            return false;
        } catch (Exception ex) {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return false;
        }
        return true;
    }

    private static final Logger log = LoggerFactory.getLogger(VariableReadWriteCallable.class);
}
