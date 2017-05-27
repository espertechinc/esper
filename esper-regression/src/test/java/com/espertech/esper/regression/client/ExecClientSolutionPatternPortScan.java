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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.deploy.DeploymentInformation;
import com.espertech.esper.client.deploy.DeploymentNotFoundException;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.client.time.CurrentTimeSpanEvent;
import com.espertech.esper.client.util.DateTime;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import static org.junit.Assert.assertFalse;

public class ExecClientSolutionPatternPortScan implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionPortScan_PrimarySuccess(epService);
        runAssertionPortScan_KeepAlerting(epService);
        runAssertionPortScan_FallsUnderThreshold(epService);
    }

    private void runAssertionPortScan_PrimarySuccess(EPServiceProvider epService) throws Exception {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        setCurrentTime(epService, "8:00:00");
        SupportUpdateListener listener = deployPortScan(epService);
        sendEventMultiple(epService, 20, "A", "B");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "type,cnt".split(","), new Object[]{"DETECTED", 20L});
        undeployRemoveAll(epService);
    }

    private void runAssertionPortScan_KeepAlerting(EPServiceProvider epService) throws Exception {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        setCurrentTime(epService, "8:00:00");
        SupportUpdateListener listener = deployPortScan(epService);
        sendEventMultiple(epService, 20, "A", "B");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "type,cnt".split(","), new Object[]{"DETECTED", 20L});

        setCurrentTime(epService, "8:00:29");
        sendEventMultiple(epService, 20, "A", "B");

        setCurrentTime(epService, "8:00:59");
        sendEventMultiple(epService, 20, "A", "B");
        assertFalse(listener.isInvoked());

        setCurrentTime(epService, "8:01:00");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "type,cnt".split(","), new Object[]{"UPDATE", 20L});

        undeployRemoveAll(epService);
    }

    private void runAssertionPortScan_FallsUnderThreshold(EPServiceProvider epService) throws Exception {
        epService.getEPRuntime().sendEvent(new CurrentTimeEvent(0));
        setCurrentTime(epService, "8:00:00");
        SupportUpdateListener listener = deployPortScan(epService);
        sendEventMultiple(epService, 20, "A", "B");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "type,cnt".split(","), new Object[]{"DETECTED", 20L});

        setCurrentTime(epService, "8:01:00");
        EPAssertionUtil.assertProps(listener.getAndResetLastNewData()[0], "type,cnt".split(","), new Object[]{"DONE", 0L});

        undeployRemoveAll(epService);
    }

    private void sendEventMultiple(EPServiceProvider epService, int count, String src, String dst) {
        for (int i = 0; i < count; i++) {
            sendEvent(epService, src, dst, 16 + i, "m" + count);
        }
    }

    private void sendEvent(EPServiceProvider epService, String src, String dst, int port, String marker) {
        epService.getEPRuntime().sendEvent(new Object[]{src, dst, port, marker}, "PortScanEvent");
    }

    private void setCurrentTime(EPServiceProvider epService, String time) {
        String timestamp = "2002-05-30T" + time + ".000";
        long current = DateTime.parseDefaultMSec(timestamp);
        System.out.println("Advancing time to " + timestamp + " msec " + current);
        epService.getEPRuntime().sendEvent(new CurrentTimeSpanEvent(current));
    }

    private SupportUpdateListener deployPortScan(EPServiceProvider epService) throws Exception {
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
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("output").addListener(listener);
        return listener;
    }

    private void undeployRemoveAll(EPServiceProvider epService) throws DeploymentNotFoundException {
        DeploymentInformation[] deployments = epService.getEPAdministrator().getDeploymentAdmin().getDeploymentInformation();
        for (DeploymentInformation deployment : deployments) {
            epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(deployment.getDeploymentId());
        }
    }
}
