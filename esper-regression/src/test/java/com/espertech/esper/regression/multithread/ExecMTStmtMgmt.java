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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportMarketDataBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.multithread.StmtMgmtCallable;

import java.util.Arrays;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Test for multithread-safety for creating and stopping various statements.
 */
public class ExecMTStmtMgmt implements RegressionExecution {
    private final static String EVENT_NAME = SupportMarketDataBean.class.getName();
    private final static Object[][] STMT = new Object[][]{
            // true for EPL, false for Pattern; Statement text
            {true, "select * from " + EVENT_NAME + " where symbol = 'IBM'"},
            {true, "select * from " + EVENT_NAME + " (symbol = 'IBM')"},
            {true, "select * from " + EVENT_NAME + " (price>1)"},
            {true, "select * from " + EVENT_NAME + " (feed='RT')"},
            {true, "select * from " + EVENT_NAME + " (symbol='IBM', price>1, feed='RT')"},
            {true, "select * from " + EVENT_NAME + " (price>1, feed='RT')"},
            {true, "select * from " + EVENT_NAME + " (symbol='IBM', feed='RT')"},
            {true, "select * from " + EVENT_NAME + " (symbol='IBM', feed='RT') where price between 0 and 1000"},
            {true, "select * from " + EVENT_NAME + " (symbol='IBM') where price between 0 and 1000 and feed='RT'"},
            {true, "select * from " + EVENT_NAME + " (symbol='IBM') where 'a'='a'"},
            {false, "every a=" + EVENT_NAME + "(symbol='IBM')"},
            {false, "every a=" + EVENT_NAME + "(symbol='IBM', price < 1000)"},
            {false, "every a=" + EVENT_NAME + "(feed='RT', price < 1000)"},
            {false, "every a=" + EVENT_NAME + "(symbol='IBM', feed='RT')"},
    };

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPatterns(epService);
        runAssertionEachStatementAlone(epService);
        runAssertionStatementsMixed(epService);
        runAssertionStatementsAll(epService);
    }

    private void runAssertionPatterns(EPServiceProvider epService) throws Exception {
        int numThreads = 3;
        Object[][] statements;

        statements = new Object[][]{STMT[10]};
        tryStatementCreateSendAndStop(epService, numThreads, statements, 10);
        epService.getEPAdministrator().destroyAllStatements();

        statements = new Object[][]{STMT[10], STMT[11]};
        tryStatementCreateSendAndStop(epService, numThreads, statements, 10);
        epService.getEPAdministrator().destroyAllStatements();

        statements = new Object[][]{STMT[10], STMT[11], STMT[12]};
        tryStatementCreateSendAndStop(epService, numThreads, statements, 10);
        epService.getEPAdministrator().destroyAllStatements();

        statements = new Object[][]{STMT[10], STMT[11], STMT[12], STMT[13]};
        tryStatementCreateSendAndStop(epService, numThreads, statements, 10);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEachStatementAlone(EPServiceProvider epService) throws Exception {
        int numThreads = 4;
        for (int i = 0; i < STMT.length; i++) {
            Object[][] statements = new Object[][]{STMT[i]};
            tryStatementCreateSendAndStop(epService, numThreads, statements, 10);
        }
    }

    private void runAssertionStatementsMixed(EPServiceProvider epService) throws Exception {
        int numThreads = 2;
        Object[][] statements = new Object[][]{STMT[1], STMT[4], STMT[6], STMT[7], STMT[8]};
        tryStatementCreateSendAndStop(epService, numThreads, statements, 10);

        statements = new Object[][]{STMT[1], STMT[7], STMT[8], STMT[11], STMT[12]};
        tryStatementCreateSendAndStop(epService, numThreads, statements, 10);
    }

    private void runAssertionStatementsAll(EPServiceProvider epService) throws Exception {
        int numThreads = 3;
        tryStatementCreateSendAndStop(epService, numThreads, STMT, 10);
    }

    private void tryStatementCreateSendAndStop(EPServiceProvider epService, int numThreads, Object[][] statements, int numRepeats) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(numThreads);
        Future[] future = new Future[numThreads];
        for (int i = 0; i < numThreads; i++) {
            Callable callable = new StmtMgmtCallable(epService, statements, numRepeats);
            future[i] = threadPool.submit(callable);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        StringBuilder statementDigest = new StringBuilder();
        for (int i = 0; i < statements.length; i++) {
            statementDigest.append(Arrays.toString(statements[i]));
        }

        for (int i = 0; i < numThreads; i++) {
            assertTrue("Failed in " + statementDigest.toString(), (Boolean) future[i].get());
        }
    }
}
