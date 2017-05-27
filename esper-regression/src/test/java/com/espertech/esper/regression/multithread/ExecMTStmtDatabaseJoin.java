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
package com.espertech.esper.regression.multithread;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationDBRef;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.epl.SupportDatabaseService;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtDatabaseJoinCallable;

import java.util.Properties;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety for database joins.
 */
public class ExecMTStmtDatabaseJoin implements RegressionExecution {
    private final static String EVENT_NAME = SupportBean.class.getName();

    public void configure(Configuration configuration) throws Exception {
        ConfigurationDBRef configDB = new ConfigurationDBRef();
        configDB.setDriverManagerConnection(SupportDatabaseService.DRIVER, SupportDatabaseService.FULLURL, new Properties());
        configDB.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.RETAIN);
        configDB.setConnectionCatalog("test");
        configDB.setConnectionReadOnly(true);
        configDB.setConnectionTransactionIsolation(1);
        configDB.setConnectionAutoCommit(true);
        configuration.addDatabaseReference("MyDB", configDB);
    }

    public void run(EPServiceProvider epService) throws Exception {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * \n" +
                "  from " + EVENT_NAME + "#length(1000) as s0,\n" +
                "      sql:MyDB ['select myvarchar from mytesttable where ${intPrimitive} = mytesttable.mybigint'] as s1"
        );
        trySendAndReceive(epService, 4, stmt, 1000);
        trySendAndReceive(epService, 2, stmt, 2000);
    }

    private void trySendAndReceive(EPServiceProvider epService, int numThreads, EPStatement statement, int numRepeats) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtDatabaseJoinCallable(epService, statement, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        for (int i = 0; i < numThreads; i++) {
            assertTrue("Failed in " + statement.getText(), (Boolean) future[i].get());
        }
    }
}
