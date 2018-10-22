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
package com.espertech.esper.regressionlib.suite.client.runtime;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.util.DateTime;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.runtime.client.scopetest.SupportUpdateListener;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class ClientRuntimeSolutionPatternPortScan {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientRuntimePortScanPrimarySuccess());
        execs.add(new ClientRuntimePortScanKeepAlerting());
        execs.add(new ClientRuntimePortScanFallsUnderThreshold());
        return execs;
    }

    private static class ClientRuntimePortScanPrimarySuccess implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            setCurrentTime(env, "8:00:00");
            SupportUpdateListener listener = deployPortScan(env);
            sendEventMultiple(env, 20, "A", "B");
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "type,cnt".split(","), new Object[]{"DETECTED", 20L});
            env.undeployAll();
        }
    }

    private static class ClientRuntimePortScanKeepAlerting implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            setCurrentTime(env, "8:00:00");
            SupportUpdateListener listener = deployPortScan(env);
            sendEventMultiple(env, 20, "A", "B");
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "type,cnt".split(","), new Object[]{"DETECTED", 20L});

            setCurrentTime(env, "8:00:29");
            sendEventMultiple(env, 20, "A", "B");

            setCurrentTime(env, "8:00:59");
            sendEventMultiple(env, 20, "A", "B");
            assertFalse(listener.isInvoked());

            setCurrentTime(env, "8:01:00");
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "type,cnt".split(","), new Object[]{"UPDATE", 20L});

            env.undeployAll();
        }
    }

    private static class ClientRuntimePortScanFallsUnderThreshold implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.advanceTime(0);
            setCurrentTime(env, "8:00:00");
            SupportUpdateListener listener = deployPortScan(env);
            sendEventMultiple(env, 20, "A", "B");
            EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "type,cnt".split(","), new Object[]{"DETECTED", 20L});

            setCurrentTime(env, "8:01:00");
            EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[0], "type,cnt".split(","), new Object[]{"DONE", 0L});

            env.undeployAll();
        }
    }

    private static void sendEventMultiple(RegressionEnvironment env, int count, String src, String dst) {
        for (int i = 0; i < count; i++) {
            sendEvent(env, src, dst, 16 + i, "m" + count);
        }
    }

    private static void sendEvent(RegressionEnvironment env, String src, String dst, int port, String marker) {
        env.sendEventObjectArray(new Object[]{src, dst, port, marker}, "PortScanEvent");
    }

    private static void setCurrentTime(RegressionEnvironment env, String time) {
        String timestamp = "2002-05-30T" + time + ".000";
        long current = DateTime.parseDefaultMSec(timestamp);
        System.out.println("Advancing time to " + timestamp + " msec " + current);
        env.advanceTimeSpan(current);
    }

    private static SupportUpdateListener deployPortScan(RegressionEnvironment env) {
        String epl =
            "create objectarray schema PortScanEvent(src string, dst string, port int, marker string);\n" +
                "\n" +
                "create table ScanCountTable(src string primary key, dst string primary key, cnt count(*), win window(*) @type(PortScanEvent));\n" +
                "\n" +
                "into table ScanCountTable\n" +
                "insert into CountStream\n" +
                "select src, dst, count(*) as cnt, window(*) as win\n" +
                "from PortScanEvent#unique(src, dst, port)#time(30 sec) group by src,dst;\n" +
                "\n" +
                "create window SituationsWindow#keepall (src string, dst string, detectionTime long);\n" +
                "\n" +
                "on CountStream(cnt >= 20) as cs\n" +
                "merge SituationsWindow sw\n" +
                "where cs.src = sw.src and cs.dst = sw.dst\n" +
                "when not matched \n" +
                "  then insert select src, dst, current_timestamp as detectionTime\n" +
                "  then insert into OutputAlerts select 'DETECTED' as type, cs.cnt as cnt, cs.win as contributors;\n" +
                "\n" +
                "on pattern [every timer:at(*, *, *, *, *)] \n" +
                "insert into OutputAlerts \n" +
                "select 'UPDATE' as type, ScanCountTable[src, dst].cnt as cnt, ScanCountTable[src, dst].win as contributors\n" +
                "from SituationsWindow sc;\n" +
                "\n" +
                "on pattern [every timer:at(*, *, *, *, *)] \n" +
                "merge SituationsWindow sw\n" +
                "when matched and (select cnt from ScanCountTable where src = sw.src and dst = sw.dst) < 10\n" +
                "  then delete\n" +
                "  then insert into OutputAlerts select 'DONE' as type, ScanCountTable[src, dst].cnt as cnt, null as contributors \n" +
                "when matched and detectionTime.after(current_timestamp, 16 hours)\n" +
                "  then delete\n" +
                "  then insert into OutputAlerts select 'EXPIRED' as type, -1L as cnt, null as contributors;\n" +
                "\n" +
                // For more output: "@audit() select * from CountStream;\n" +
                "@name('output') select * from OutputAlerts;\n";
        EPCompiled compiled = env.compileWBusPublicType(epl);
        env.deploy(compiled);
        SupportUpdateListener listener = new SupportUpdateListener();
        env.statement("output").addListener(listener);
        return listener;
    }
}
