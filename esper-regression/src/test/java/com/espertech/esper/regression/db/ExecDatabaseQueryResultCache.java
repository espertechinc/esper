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
package com.espertech.esper.regression.db;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExecDatabaseQueryResultCache implements RegressionExecution {
    private final boolean lru;
    private final Integer lruSize;
    private final Double expiryMaxAgeSeconds;
    private final Double expiryPurgeIntervalSeconds;
    private final long assertMaximumTime;
    private final int numEvents;
    private final boolean useRandomKeyLookup;

    public ExecDatabaseQueryResultCache(boolean lru, Integer lruSize, Double expiryMaxAgeSeconds, Double expiryPurgeIntervalSeconds, long assertMaximumTime, int numEvents, boolean useRandomKeyLookup) {
        this.lru = lru;
        this.lruSize = lruSize;
        this.expiryMaxAgeSeconds = expiryMaxAgeSeconds;
        this.expiryPurgeIntervalSeconds = expiryPurgeIntervalSeconds;
        this.assertMaximumTime = assertMaximumTime;
        this.numEvents = numEvents;
        this.useRandomKeyLookup = useRandomKeyLookup;
    }

    public void configure(Configuration configuration) throws Exception {
        ConfigurationDBRef configDB = getDefaultConfig();
        if (lru) {
            configDB.setLRUCache(lruSize);
        } else {
            configDB.setExpiryTimeCache(expiryMaxAgeSeconds, expiryPurgeIntervalSeconds);
        }
        configuration.addDatabaseReference("MyDB", configDB);
    }

    public void run(EPServiceProvider epService) throws Exception {
        tryCache(epService, assertMaximumTime, numEvents, useRandomKeyLookup);
    }

    private void tryCache(EPServiceProvider epService, long assertMaximumTime, int numEvents, boolean useRandomLookupKey) {
        long startTime = System.currentTimeMillis();
        trySendEvents(epService, numEvents, useRandomLookupKey);
        long endTime = System.currentTimeMillis();
        log.info(".tryCache delta=" + (endTime - startTime));
        assertTrue(endTime - startTime < assertMaximumTime);
    }

    private void trySendEvents(EPServiceProvider engine, int numEvents, boolean useRandomLookupKey) {
        Random random = new Random();
        String stmtText = "select myint from " +
                SupportBean_S0.class.getName() + " as s0," +
                " sql:MyDB ['select myint from mytesttable where ${id} = mytesttable.mybigint'] as s1";

        EPStatement statement = engine.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        listener = new SupportUpdateListener();
        statement.addListener(listener);

        log.debug(".trySendEvents Sending " + numEvents + " events");
        for (int i = 0; i < numEvents; i++) {
            int id = 0;
            if (useRandomLookupKey) {
                id = random.nextInt(1000);
            } else {
                id = i % 10 + 1;
            }

            SupportBean_S0 bean = new SupportBean_S0(id);
            engine.getEPRuntime().sendEvent(bean);

            if ((!useRandomLookupKey) || ((id >= 1) && (id <= 10))) {
                EventBean received = listener.assertOneGetNewAndReset();
                assertEquals(id * 10, received.get("myint"));
            }
        }

        log.debug(".trySendEvents Stopping statement");
        statement.stop();
    }

    private ConfigurationDBRef getDefaultConfig() {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        return configDB;
    }

    private static final Logger log = LoggerFactory.getLogger(ExecDatabaseQueryResultCache.class);
}
