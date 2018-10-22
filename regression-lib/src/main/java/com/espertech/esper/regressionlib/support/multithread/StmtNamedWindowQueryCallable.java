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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetPreparedQueryParameterized;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

public class StmtNamedWindowQueryCallable implements Callable {
    private final RegressionEnvironment env;
    private final RegressionPath path;
    private final int numRepeats;
    private final String threadKey;

    public StmtNamedWindowQueryCallable(RegressionEnvironment env, RegressionPath path, int numRepeats, String threadKey) {
        this.env = env;
        this.path = path;
        this.numRepeats = numRepeats;
        this.threadKey = threadKey;
    }

    public Object call() throws Exception {

        String selectQuery = "select * from MyWindow where theString='" + threadKey + "' and longPrimitive=?::int";
        EPCompiled compiled = env.compileFAF(selectQuery, path);
        EPFireAndForgetPreparedQueryParameterized prepared = env.runtime().getFireAndForgetService().prepareQueryWithParameters(compiled);

        try {
            long total = 0;
            for (int loop = 0; loop < numRepeats; loop++) {
                // Insert event into named window
                sendMarketBean(threadKey, loop);
                total++;

                prepared.setObject(1, loop);
                EPFireAndForgetQueryResult queryResult = env.runtime().getFireAndForgetService().executeQuery(prepared);
                Assert.assertEquals(1, queryResult.getArray().length);
                Assert.assertEquals(threadKey, queryResult.getArray()[0].get("theString"));
                Assert.assertEquals((long) loop, queryResult.getArray()[0].get("longPrimitive"));
            }
        } catch (Exception ex) {
            log.error("Error in thread " + Thread.currentThread().getId(), ex);
            return false;
        }
        return true;
    }

    private void sendMarketBean(String symbol, long volume) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0, volume, "");
        env.sendEventBean(bean);
    }

    private static final Logger log = LoggerFactory.getLogger(StmtNamedWindowQueryCallable.class);
}
