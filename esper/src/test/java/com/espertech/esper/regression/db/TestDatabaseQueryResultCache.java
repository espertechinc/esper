/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.db;

import com.espertech.esper.client.scopetest.SupportUpdateListener;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.support.epl.SupportDatabaseService;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.client.EventBean;

import java.util.Properties;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDatabaseQueryResultCache extends TestCase
{
    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    protected void tearDown() throws Exception {
        listener = null;
        epService.destroy();
    }

    public void testExpireCacheNoPurge()
    {
        ConfigurationDBRef configDB = getDefaultConfig();
        configDB.setExpiryTimeCache(1.0d, Double.MAX_VALUE);
        tryCache(configDB, 5000, 1000, false);
    }

    public void testLRUCache()
    {
        ConfigurationDBRef configDB = getDefaultConfig();
        configDB.setLRUCache(100);
        tryCache(configDB, 2000, 1000, false);
    }

    public void testLRUCache25k()
    {
        ConfigurationDBRef configDB = getDefaultConfig();
        configDB.setLRUCache(100);
        tryCache(configDB, 7000, 25000, false);
    }

    public void testExpireCache25k()
    {
        ConfigurationDBRef configDB = getDefaultConfig();
        configDB.setExpiryTimeCache(2, 2);
        tryCache(configDB, 7000, 25000, false);
    }

    public void testExpireRandomKeys()
    {
        ConfigurationDBRef configDB = getDefaultConfig();
        configDB.setExpiryTimeCache(1, 1);
        tryCache(configDB, 7000, 25000, true);
    }

    private void tryCache(ConfigurationDBRef configDB, long assertMaximumTime, int numEvents, boolean useRandomLookupKey)
    {
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addDatabaseReference("MyDB", configDB);

        epService = EPServiceProviderManager.getProvider("TestDatabaseQueryResultCache", configuration);
        epService.initialize();

        long startTime = System.currentTimeMillis();
        trySendEvents(epService, numEvents, useRandomLookupKey);
        long endTime = System.currentTimeMillis();
        log.info(".tryCache " + configDB.getDataCacheDesc() + " delta=" + (endTime - startTime));
        assertTrue(endTime - startTime < assertMaximumTime);
    }

    private void trySendEvents(EPServiceProvider engine, int numEvents, boolean useRandomLookupKey)
    {
        Random random = new Random();
        String stmtText = "select myint from " +
                SupportBean_S0.class.getName() + " as s0," +
                " sql:MyDB ['select myint from mytesttable where ${id} = mytesttable.mybigint'] as s1";

        EPStatement statement = engine.getEPAdministrator().createEPL(stmtText);
        listener = new SupportUpdateListener();
        statement.addListener(listener);

        log.debug(".trySendEvents Sending " + numEvents + " events");
        for (int i = 0; i < numEvents; i++)
        {
            int id = 0;
            if (useRandomLookupKey)
            {
                id = random.nextInt(1000);
            }
            else
            {
                id = i % 10 + 1;
            }

            SupportBean_S0 bean = new SupportBean_S0(id);
            engine.getEPRuntime().sendEvent(bean);

            if ((!useRandomLookupKey) || ((id >= 1) && (id <= 10)))
            {
                EventBean received = listener.assertOneGetNewAndReset();
                assertEquals(id * 10, received.get("myint"));
            }
        }

        log.debug(".trySendEvents Stopping statement");
        statement.stop();
    }

    private ConfigurationDBRef getDefaultConfig()
    {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        return configDB;
    }

    private static final Logger log = LoggerFactory.getLogger(TestDatabaseQueryResultCache.class);
}
