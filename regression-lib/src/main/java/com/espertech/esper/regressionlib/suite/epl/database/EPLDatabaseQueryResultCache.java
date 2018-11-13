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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

import static org.junit.Assert.assertTrue;

public class EPLDatabaseQueryResultCache implements RegressionExecution {

    private final boolean lru;
    private final Integer lruSize;
    private final Double expiryMaxAgeSeconds;
    private final Double expiryPurgeIntervalSeconds;
    private final long assertMaximumTime;
    private final int numEvents;
    private final boolean useRandomKeyLookup;

    @Override
    public boolean excludeWhenInstrumented() {
        return true;
    }

    public EPLDatabaseQueryResultCache(boolean lru, Integer lruSize, Double expiryMaxAgeSeconds, Double expiryPurgeIntervalSeconds, long assertMaximumTime, int numEvents, boolean useRandomKeyLookup) {
        this.lru = lru;
        this.lruSize = lruSize;
        this.expiryMaxAgeSeconds = expiryMaxAgeSeconds;
        this.expiryPurgeIntervalSeconds = expiryPurgeIntervalSeconds;
        this.assertMaximumTime = assertMaximumTime;
        this.numEvents = numEvents;
        this.useRandomKeyLookup = useRandomKeyLookup;
    }

    public void run(RegressionEnvironment env) {
        tryCache(env, assertMaximumTime, numEvents, useRandomKeyLookup);
    }

    private static void tryCache(RegressionEnvironment env, long assertMaximumTime, int numEvents, boolean useRandomLookupKey) {
        long startTime = System.currentTimeMillis();
        trySendEvents(env, numEvents, useRandomLookupKey);
        long endTime = System.currentTimeMillis();
        log.info(".tryCache delta=" + (endTime - startTime));
        assertTrue(endTime - startTime < assertMaximumTime);
    }

    private static void trySendEvents(RegressionEnvironment env, int numEvents, boolean useRandomLookupKey) {
        Random random = new Random();
        String stmtText = "@name('s0') select myint from " +
            "SupportBean_S0 as s0," +
            " sql:MyDB ['select myint from mytesttable where ${id} = mytesttable.mybigint'] as s1";
        env.compileDeploy(stmtText).addListener("s0");

        log.debug(".trySendEvents Sending " + numEvents + " events");
        for (int i = 0; i < numEvents; i++) {
            int id = 0;
            if (useRandomLookupKey) {
                id = random.nextInt(1000);
            } else {
                id = i % 10 + 1;
            }

            SupportBean_S0 bean = new SupportBean_S0(id);
            env.sendEventBean(bean);

            if ((!useRandomLookupKey) || ((id >= 1) && (id <= 10))) {
                EventBean received = env.listener("s0").assertOneGetNewAndReset();
                Assert.assertEquals(id * 10, received.get("myint"));
            }
        }

        log.debug(".trySendEvents Stopping statement");
        env.undeployAll();
    }

    public boolean isLru() {
        return lru;
    }

    public Integer getLruSize() {
        return lruSize;
    }

    public Double getExpiryMaxAgeSeconds() {
        return expiryMaxAgeSeconds;
    }

    public Double getExpiryPurgeIntervalSeconds() {
        return expiryPurgeIntervalSeconds;
    }

    private static final Logger log = LoggerFactory.getLogger(EPLDatabaseQueryResultCache.class);
}
