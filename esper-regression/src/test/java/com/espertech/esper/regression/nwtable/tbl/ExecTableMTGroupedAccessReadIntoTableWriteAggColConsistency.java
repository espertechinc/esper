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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ExecTableMTGroupedAccessReadIntoTableWriteAggColConsistency implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecTableMTGroupedAccessReadIntoTableWriteAggColConsistency.class);

    /**
     * Table:
     * create table vartotal (key string primary key, tc0 sum(int), tc1 sum(int) ... tc9 sum(int))
     * <p>
     * Seed the table with a number of groups, no new ones are added or deleted during the test.
     * For a given number of seconds and a given number of groups:
     * - Single writer updates a group (round-robin), each group associates with 10 columns .
     * - N readers pull a group's columns, round-robin, check that all 10 values are consistent.
     * - The 10 values are sum-int totals that are expected to all have the same value.
     */
    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType(Local10ColEvent.class);
        configuration.addEventType(SupportBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        tryMT(epService, 10, 3);
    }

    private void tryMT(EPServiceProvider epService, int numGroups, int numSeconds) throws Exception {
        String eplCreateVariable = "create table vartotal (key string primary key, " + CollectionUtil.toString(getDeclareCols()) + ")";
        epService.getEPAdministrator().createEPL(eplCreateVariable);

        String eplInto = "into table vartotal select " + CollectionUtil.toString(getIntoCols()) + " from Local10ColEvent group by groupKey";
        epService.getEPAdministrator().createEPL(eplInto);

        // initialize groups
        String[] groups = new String[numGroups];
        for (int i = 0; i < numGroups; i++) {
            groups[i] = "G" + i;
            epService.getEPRuntime().sendEvent(new Local10ColEvent(groups[i], 0));
        }

        WriteRunnable writeRunnable = new WriteRunnable(epService, groups);
        ReadRunnable readRunnable = new ReadRunnable(epService, groups);

        // start
        Thread t1 = new Thread(writeRunnable);
        Thread t2 = new Thread(readRunnable);
        t1.start();
        t2.start();

        // wait
        Thread.sleep(numSeconds * 1000);

        // shutdown
        writeRunnable.setShutdown(true);
        readRunnable.setShutdown(true);

        // join
        log.info("Waiting for completion");
        t1.join();
        t2.join();

        assertNull(writeRunnable.getException());
        assertNull(readRunnable.getException());
        assertTrue(writeRunnable.numEvents > 100);
        assertTrue(readRunnable.numQueries > 100);
        System.out.println("Send " + writeRunnable.numEvents + " and performed " + readRunnable.numQueries + " reads");
    }

    private Collection<String> getDeclareCols() {
        List<String> cols = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {  // 10 columns, not configurable
            cols.add("tc" + i + " sum(int)");
        }
        return cols;
    }

    private Collection<String> getIntoCols() {
        List<String> cols = new ArrayList<String>();
        for (int i = 0; i < 10; i++) {  // 10 columns, not configurable
            cols.add("sum(c" + i + ") as tc" + i);
        }
        return cols;
    }

    public static class WriteRunnable implements Runnable {

        private final EPServiceProvider epService;
        private final String[] groups;

        private RuntimeException exception;
        private boolean shutdown;
        private int numEvents;

        public WriteRunnable(EPServiceProvider epService, String[] groups) {
            this.epService = epService;
            this.groups = groups;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for write");

            try {
                while (!shutdown) {
                    int groupNum = numEvents % groups.length;
                    epService.getEPRuntime().sendEvent(new Local10ColEvent(groups[groupNum], numEvents));
                    numEvents++;
                }
            } catch (RuntimeException ex) {
                log.error("Exception encountered: " + ex.getMessage(), ex);
                exception = ex;
            }

            log.info("Completed event send for write");
        }

        public RuntimeException getException() {
            return exception;
        }
    }

    public static class ReadRunnable implements Runnable {

        private final EPServiceProvider epService;
        private final String[] groups;

        private RuntimeException exception;
        private boolean shutdown;
        private int numQueries;

        public ReadRunnable(EPServiceProvider epService, String[] groups) {
            this.epService = epService;
            this.groups = groups;
        }

        public void setShutdown(boolean shutdown) {
            this.shutdown = shutdown;
        }

        public void run() {
            log.info("Started event send for read");

            try {
                String eplSelect = "select vartotal[theString] as out from SupportBean";
                SupportUpdateListener listener = new SupportUpdateListener();
                epService.getEPAdministrator().createEPL(eplSelect).addListener(listener);

                while (!shutdown) {
                    int groupNum = numQueries % groups.length;
                    epService.getEPRuntime().sendEvent(new SupportBean(groups[groupNum], 0));
                    EventBean event = listener.assertOneGetNewAndReset();
                    assertEvent((Map) event.get("out"));
                    numQueries++;
                }
            } catch (RuntimeException ex) {
                log.error("Exception encountered: " + ex.getMessage(), ex);
                exception = ex;
            }

            log.info("Completed event send for read");
        }

        private void assertEvent(Map info) {
            Object tc0 = info.get("tc0");
            for (int i = 1; i < 10; i++) {
                assertEquals(tc0, info.get("tc" + i));
            }
        }

        public RuntimeException getException() {
            return exception;
        }
    }

    public static final class Local10ColEvent {
        private final String groupKey;
        private final int c0;
        private final int c1;
        private final int c2;
        private final int c3;
        private final int c4;
        private final int c5;
        private final int c6;
        private final int c7;
        private final int c8;
        private final int c9;

        public Local10ColEvent(String groupKey, int value) {
            this.groupKey = groupKey;
            this.c0 = value;
            this.c1 = value;
            this.c2 = value;
            this.c3 = value;
            this.c4 = value;
            this.c5 = value;
            this.c6 = value;
            this.c7 = value;
            this.c8 = value;
            this.c9 = value;
        }

        public String getGroupKey() {
            return groupKey;
        }

        public int getC0() {
            return c0;
        }

        public int getC1() {
            return c1;
        }

        public int getC2() {
            return c2;
        }

        public int getC3() {
            return c3;
        }

        public int getC4() {
            return c4;
        }

        public int getC5() {
            return c5;
        }

        public int getC6() {
            return c6;
        }

        public int getC7() {
            return c7;
        }

        public int getC8() {
            return c8;
        }

        public int getC9() {
            return c9;
        }
    }
}
