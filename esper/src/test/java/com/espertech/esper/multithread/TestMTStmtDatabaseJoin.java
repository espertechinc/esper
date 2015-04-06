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

package com.espertech.esper.multithread;

import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.epl.SupportDatabaseService;
import com.espertech.esper.support.client.SupportConfigFactory;

import java.util.concurrent.*;
import java.util.Properties;

/**
 * Test for multithread-safety for database joins.
 *
 */
public class TestMTStmtDatabaseJoin extends TestCase
{
    private EPServiceProvider engine;

    private final static String EVENT_NAME = SupportBean.class.getName();

    public void setUp()
    {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        configDB.setConnectionCatalog("test");
        configDB.setConnectionReadOnly(true);
        configDB.setConnectionTransactionIsolation(1);
        configDB.setConnectionAutoCommit(true);
        Configuration configuration = SupportConfigFactory.getConfiguration();
        configuration.addDatabaseReference("MyDB", configDB);

        engine = EPServiceProviderManager.getProvider("TestMTStmtDatabaseJoin", configuration);        
    }

    public void tearDown()
    {
        engine.destroy();
    }

    public void testJoin() throws Exception
    {
        EPStatement stmt = engine.getEPAdministrator().createEPL("select * \n" +
                "  from " + EVENT_NAME + ".win:length(1000) as s0,\n" +
                "      sql:MyDB ['select myvarchar from mytesttable where ${intPrimitive} = mytesttable.mybigint'] as s1"
                );
        trySendAndReceive(4, stmt, 1000);
        trySendAndReceive(2, stmt, 2000);
    }

    private void trySendAndReceive(int numThreads, EPStatement statement, int numRepeats) throws Exception
    {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future future[] = new Future[numThreads];
        for (int i = 0; i < numThreads; i++)
        {
            Callable callable = new StmtDatabaseJoinCallable(engine, statement, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++)
        {
            assertTrue("Failed in " + statement.getText(), (Boolean) future[i].get());
        }
    }
}
