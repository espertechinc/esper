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

package com.espertech.esper.regression.nwtable;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBean_S0;
import com.espertech.esper.support.client.SupportConfigFactory;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestTableMTGroupedWContextIntoTableWriteAsSharedTable extends TestCase
{
    private static final Log log = LogFactory.getLog(TestTableMTGroupedWContextIntoTableWriteAsSharedTable.class);

    private EPServiceProvider epService;

    public void setUp()
    {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportBean", SupportBean.class);
        config.addEventType("SupportBean_S0", SupportBean_S0.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
    }

    /**
     * Multiple writers share a key space that they aggregate into.
     * Writer utilize a hash partition context.
     * After all writers are done validate the space.
     */
    public void testMT() throws Exception
    {
        // with T, N, G:  Each of T threads loops N times and sends for each loop G events for each group.
        // for a total of T*N*G events being processed, and G aggregations retained in a shared variable.
        // Group is the innermost loop.
        tryMT(8, 1000, 64);
    }

    private void tryMT(int numThreads, int numLoops, int numGroups) throws Exception
    {
        String eplDeclare =
                "create table varTotal (key string primary key, total sum(int));\n" +
                "create context ByStringHash\n" +
                "  coalesce by consistent_hash_crc32(theString) from SupportBean granularity 16 preallocate\n;" +
                "context ByStringHash into table varTotal select theString, sum(intPrimitive) as total from SupportBean group by theString;\n";
        String eplAssert = "select varTotal[p00].total as c0 from SupportBean_S0";

        runAndAssert(epService, eplDeclare, eplAssert, numThreads, numLoops, numGroups);
    }

    protected static void runAndAssert(EPServiceProvider epService, String eplDeclare, String eplAssert, int numThreads, int numLoops, int numGroups) throws Exception {
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(eplDeclare);

        // setup readers
        Thread[] writeThreads = new Thread[numThreads];
        WriteRunnable[] writeRunnables = new WriteRunnable[numThreads];
        for (int i = 0; i < writeThreads.length; i++) {
            writeRunnables[i] = new WriteRunnable(epService, numLoops, numGroups);
            writeThreads[i] = new Thread(writeRunnables[i]);
        }

        // start
        for (Thread writeThread : writeThreads) {
            writeThread.start();
        }

        // join
        log.info("Waiting for completion");
        for (Thread writeThread : writeThreads) {
            writeThread.join();
        }

        // assert
        for (WriteRunnable writeRunnable : writeRunnables) {
            assertNull(writeRunnable.getException());
        }

        // each group should total up to "numLoops*numThreads"
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(eplAssert).addListener(listener);
        Integer expected = numLoops * numThreads;
        for (int i = 0; i < numGroups; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean_S0(0, "G" + i));
            assertEquals(expected, listener.assertOneGetNewAndReset().get("c0"));
        }
    }

    public static class WriteRunnable implements Runnable {

        private final EPServiceProvider epService;
        private final int numGroups;
        private final int numLoops;

        private RuntimeException exception;

        public WriteRunnable(EPServiceProvider epService, int numLoops, int numGroups) {
            this.epService = epService;
            this.numGroups = numGroups;
            this.numLoops = numLoops;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                for (int i = 0; i < numLoops; i++) {
                    for (int j = 0; j < numGroups; j++) {
                        epService.getEPRuntime().sendEvent(new SupportBean("G" + j, 1));
                    }
                }
            }
            catch (RuntimeException ex) {
                log.error("Exception encountered: " + ex.getMessage(), ex);
                exception = ex;
            }

            log.info("Completed event send for write");
        }

        public RuntimeException getException() {
            return exception;
        }
    }
}
