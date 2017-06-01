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

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Collections;

public class ExecTableDocSamples implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class,
            TrafficEvent.class, IntrusionEvent.class, MyEvent.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionIncreasingUseCase(epService);
        runAssertionDoc(epService);
    }

    private void runAssertionIncreasingUseCase(EPServiceProvider epService) throws Exception {
        String epl =
                "create schema ValueEvent(value long);\n" +
                        "create schema ResetEvent(startThreshold long);\n" +
                        "create table CurrentMaxTable(currentThreshold long);\n" +
                        "@name('trigger') insert into ThresholdTriggered select * from ValueEvent(value >= CurrentMaxTable.currentThreshold);\n" +
                        "on ResetEvent merge CurrentMaxTable when matched then update set currentThreshold = startThreshold when not matched then insert select startThreshold as currentThreshold;\n" +
                        "on ThresholdTriggered update CurrentMaxTable set currentThreshold = value + 100;\n";
        DeploymentResult d = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);

        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().getStatement("trigger").addListener(listener);

        epService.getEPRuntime().sendEvent(Collections.singletonMap("startThreshold", 100L), "ResetEvent");
        epService.getEPRuntime().sendEvent(Collections.singletonMap("value", 30L), "ValueEvent");
        epService.getEPRuntime().sendEvent(Collections.singletonMap("value", 99L), "ValueEvent");
        epService.getEPRuntime().sendEvent(Collections.singletonMap("value", 100L), "ValueEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "value".split(","), new Object[]{100L});

        epService.getEPRuntime().sendEvent(Collections.singletonMap("value", 101L), "ValueEvent");
        epService.getEPRuntime().sendEvent(Collections.singletonMap("value", 103L), "ValueEvent");
        epService.getEPRuntime().sendEvent(Collections.singletonMap("value", 130L), "ValueEvent");
        epService.getEPRuntime().sendEvent(Collections.singletonMap("value", 199L), "ValueEvent");
        epService.getEPRuntime().sendEvent(Collections.singletonMap("value", 200L), "ValueEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "value".split(","), new Object[]{200L});

        epService.getEPRuntime().sendEvent(Collections.singletonMap("value", 201L), "ValueEvent");
        epService.getEPRuntime().sendEvent(Collections.singletonMap("value", 260L), "ValueEvent");
        epService.getEPRuntime().sendEvent(Collections.singletonMap("value", 301L), "ValueEvent");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "value".split(","), new Object[]{301L});

        epService.getEPAdministrator().getDeploymentAdmin().undeployRemove(d.getDeploymentId());
    }

    private void runAssertionDoc(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create table agg_srcdst as (key0 string primary key, key1 string primary key, cnt count(*))");
        epService.getEPAdministrator().createEPL("create schema IPAddressFirewallAlert(ip_src string, ip_dst string)");
        epService.getEPAdministrator().createEPL("select agg_srcdst[ip_src, ip_dst].cnt from IPAddressFirewallAlert");
        epService.getEPAdministrator().createEPL("create schema PortScanEvent(ip_src string, ip_dst string)");
        epService.getEPAdministrator().createEPL("into table agg_srcdst select count(*) as cnt from PortScanEvent group by ip_src, ip_dst");

        epService.getEPAdministrator().createEPL("create table MyStats (\n" +
                "  myKey string primary key,\n" +
                "  myAvedev avedev(int), // column holds a mean deviation of int-typed values\n" +
                "  myAvg avg(double), // column holds a average of double-typed values\n" +
                "  myCount count(*), // column holds a number of values\n" +
                "  myMax max(int), // column holds a highest int-typed value\n" +
                "  myMedian median(float), // column holds the median of float-typed values\n" +
                "  myStddev stddev(java.math.BigDecimal), // column holds a standard deviation for BigDecimal values\n" +
                "  mySum sum(long), // column holds a sum of long values\n" +
                "  myFirstEver firstever(string), // column holds the first ever string value\n" +
                "  myCountEver countever(*) // column holds the count-ever\n" +
                ")");
        epService.getEPAdministrator().createEPL("create table MyStatsMore (\n" +
                "  myKey string primary key,\n" +
                "  myAvgFiltered avg(double, boolean), // column holds a average of double-typed values\n" +
                "                      // and filtered by a boolean expression to be provided\n" +
                "  myAvgDistinct avg(distinct double) // column holds a average of distinct double-typed values\n" +
                ")");
        epService.getEPAdministrator().getConfiguration().addEventType(MyEvent.class);
        epService.getEPAdministrator().createEPL("create table MyEventAggregationTable (\n" +
                "  myKey string primary key,\n" +
                "  myWindow window(*) @type(MyEvent), // column holds a window of MyEvent events\n" +
                "  mySorted sorted(mySortValue) @type(MyEvent), // column holds MyEvent events sorted by mySortValue\n" +
                "  myMaxByEver maxbyever(mySortValue) @type(MyEvent) // column holds the single MyEvent event that \n" +
                "        // provided the highest value of mySortValue ever\n" +
                ")");

        epService.getEPAdministrator().createEPL("create context NineToFive start (0, 9, *, *, *) end (0, 17, *, *, *)");
        epService.getEPAdministrator().createEPL("context NineToFive create table AverageSpeedTable (carId string primary key, avgSpeed avg(double))");
        epService.getEPAdministrator().createEPL("context NineToFive into table AverageSpeedTable select avg(speed) as avgSpeed from TrafficEvent group by carId");

        epService.getEPAdministrator().createEPL("create table IntrusionCountTable (\n" +
                "  fromAddress string primary key,\n" +
                "  toAddress string primary key,\n" +
                "  countIntrusion10Sec count(*),\n" +
                "  countIntrusion60Sec count(*)," +
                "  active boolean\n" +
                ")");
        epService.getEPAdministrator().createEPL("into table IntrusionCountTable\n" +
                "select count(*) as countIntrusion10Sec\n" +
                "from IntrusionEvent#time(10)\n" +
                "group by fromAddress, toAddress");
        epService.getEPAdministrator().createEPL("into table IntrusionCountTable\n" +
                "select count(*) as countIntrusion60Sec\n" +
                "from IntrusionEvent#time(60)\n" +
                "group by fromAddress, toAddress");

        epService.getEPAdministrator().createEPL("create table TotalIntrusionCountTable (totalIntrusions count(*))");
        epService.getEPAdministrator().createEPL("into table TotalIntrusionCountTable select count(*) as totalIntrusions from IntrusionEvent");
        epService.getEPAdministrator().createEPL("expression alias totalIntrusions {count(*)}\n" +
                "select totalIntrusions from IntrusionEvent");
        epService.getEPAdministrator().createEPL("select TotalIntrusionCountTable.totalIntrusions from pattern[every timer:interval(60 sec)]");

        epService.getEPAdministrator().createEPL("create table MyTable (\n" +
                "theWindow window(*) @type(MyEvent),\n" +
                "theSorted sorted(mySortValue) @type(MyEvent)\n" +
                ")");
        epService.getEPAdministrator().createEPL("select MyTable.theWindow.first(), MyTable.theSorted.maxBy() from SupportBean");

        epService.getEPAdministrator().createEPL("select\n" +
                "  (select * from IntrusionCountTable as intr\n" +
                "   where intr.fromAddress = firewall.fromAddress and intr.toAddress = firewall.toAddress) \n" +
                "from IntrusionEvent as firewall");
        epService.getEPAdministrator().createEPL("select * from IntrusionCountTable as intr, IntrusionEvent as firewall\n" +
                "where intr.fromAddress = firewall.fromAddress and intr.toAddress = firewall.toAddress");

        epService.getEPAdministrator().createEPL("create table MyWindowTable (theWindow window(*) @type(MyEvent))");
        epService.getEPAdministrator().createEPL("select theWindow.first(), theWindow.last(), theWindow.window() from MyEvent, MyWindowTable");
    }

    public static class MyEvent {
        private int mySortValue;

        public int getMySortValue() {
            return mySortValue;
        }
    }

    public static class TrafficEvent {
        private String carId;
        private double speed;

        public String getCarId() {
            return carId;
        }

        public double getSpeed() {
            return speed;
        }
    }

    public static class IntrusionEvent {
        private String fromAddress;
        private String toAddress;

        public String getFromAddress() {
            return fromAddress;
        }

        public String getToAddress() {
            return toAddress;
        }
    }
}
