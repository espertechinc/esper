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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableDocSamples {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraIncreasingUseCase());
        execs.add(new InfraDoc());
        return execs;
    }

    private static class InfraIncreasingUseCase implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl =
                "create schema ValueEvent(value long);\n" +
                    "create schema ResetEvent(startThreshold long);\n" +
                    "create table CurrentMaxTable(currentThreshold long);\n" +
                    "@name('s0') insert into ThresholdTriggered select * from ValueEvent(value >= CurrentMaxTable.currentThreshold);\n" +
                    "on ResetEvent merge CurrentMaxTable when matched then update set currentThreshold = startThreshold when not matched then insert select startThreshold as currentThreshold;\n" +
                    "on ThresholdTriggered update CurrentMaxTable set currentThreshold = value + 100;\n";
            env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

            env.sendEventMap(Collections.singletonMap("startThreshold", 100L), "ResetEvent");
            env.sendEventMap(Collections.singletonMap("value", 30L), "ValueEvent");
            env.sendEventMap(Collections.singletonMap("value", 99L), "ValueEvent");

            env.milestone(0);

            env.sendEventMap(Collections.singletonMap("value", 100L), "ValueEvent");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "value".split(","), new Object[]{100L});

            env.sendEventMap(Collections.singletonMap("value", 101L), "ValueEvent");
            env.sendEventMap(Collections.singletonMap("value", 103L), "ValueEvent");
            env.sendEventMap(Collections.singletonMap("value", 130L), "ValueEvent");
            env.sendEventMap(Collections.singletonMap("value", 199L), "ValueEvent");

            env.milestone(1);

            env.sendEventMap(Collections.singletonMap("value", 200L), "ValueEvent");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "value".split(","), new Object[]{200L});

            env.sendEventMap(Collections.singletonMap("value", 201L), "ValueEvent");
            env.sendEventMap(Collections.singletonMap("value", 260L), "ValueEvent");
            env.sendEventMap(Collections.singletonMap("value", 301L), "ValueEvent");
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "value".split(","), new Object[]{301L});

            env.undeployAll();
        }
    }

    private static class InfraDoc implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table agg_srcdst as (key0 string primary key, key1 string primary key, cnt count(*))", path);
            env.compileDeploy("create schema IPAddressFirewallAlert(ip_src string, ip_dst string)", path);
            env.compileDeploy("select agg_srcdst[ip_src, ip_dst].cnt from IPAddressFirewallAlert", path);
            env.compileDeploy("create schema PortScanEvent(ip_src string, ip_dst string)", path);
            env.compileDeploy("into table agg_srcdst select count(*) as cnt from PortScanEvent group by ip_src, ip_dst", path);

            env.compileDeploy("create table MyStats (\n" +
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
                ")", path);

            env.compileDeploy("create table MyStatsMore (\n" +
                "  myKey string primary key,\n" +
                "  myAvgFiltered avg(double, boolean), // column holds a average of double-typed values\n" +
                "                      // and filtered by a boolean expression to be provided\n" +
                "  myAvgDistinct avg(distinct double) // column holds a average of distinct double-typed values\n" +
                ")", path);

            env.compileDeploy("create table MyEventAggregationTable (\n" +
                "  myKey string primary key,\n" +
                "  myWindow window(*) @type(SupportMySortValueEvent), // column holds a window of SupportMySortValueEvent events\n" +
                "  mySorted sorted(mySortValue) @type(SupportMySortValueEvent), // column holds SupportMySortValueEvent events sorted by mySortValue\n" +
                "  myMaxByEver maxbyever(mySortValue) @type(SupportMySortValueEvent) // column holds the single SupportMySortValueEvent event that \n" +
                "        // provided the highest value of mySortValue ever\n" +
                ")", path);

            env.compileDeploy("create context NineToFive start (0, 9, *, *, *) end (0, 17, *, *, *)", path);
            env.compileDeploy("context NineToFive create table AverageSpeedTable (carId string primary key, avgSpeed avg(double))", path);
            env.compileDeploy("context NineToFive into table AverageSpeedTable select avg(speed) as avgSpeed from SupportTrafficEvent group by carId", path);

            env.compileDeploy("create table IntrusionCountTable (\n" +
                "  fromAddress string primary key,\n" +
                "  toAddress string primary key,\n" +
                "  countIntrusion10Sec count(*),\n" +
                "  countIntrusion60Sec count(*)," +
                "  active boolean\n" +
                ")", path);
            env.compileDeploy("into table IntrusionCountTable\n" +
                "select count(*) as countIntrusion10Sec\n" +
                "from SupportIntrusionEvent#time(10)\n" +
                "group by fromAddress, toAddress", path);
            env.compileDeploy("into table IntrusionCountTable\n" +
                "select count(*) as countIntrusion60Sec\n" +
                "from SupportIntrusionEvent#time(60)\n" +
                "group by fromAddress, toAddress", path);

            env.compileDeploy("create table TotalIntrusionCountTable (totalIntrusions count(*))", path);
            env.compileDeploy("into table TotalIntrusionCountTable select count(*) as totalIntrusions from SupportIntrusionEvent", path);
            env.compileDeploy("expression alias totalIntrusions {count(*)}\n" +
                "select totalIntrusions from SupportIntrusionEvent", path);
            env.compileDeploy("select TotalIntrusionCountTable.totalIntrusions from pattern[every timer:interval(60 sec)]", path);

            env.compileDeploy("create table MyTable (\n" +
                "theWindow window(*) @type(SupportMySortValueEvent),\n" +
                "theSorted sorted(mySortValue) @type(SupportMySortValueEvent)\n" +
                ")", path);
            env.compileDeploy("select MyTable.theWindow.first(), MyTable.theSorted.maxBy() from SupportBean", path);

            env.compileDeploy("select\n" +
                "  (select * from IntrusionCountTable as intr\n" +
                "   where intr.fromAddress = firewall.fromAddress and intr.toAddress = firewall.toAddress) \n" +
                "from SupportIntrusionEvent as firewall", path);
            env.compileDeploy("select * from IntrusionCountTable as intr, SupportIntrusionEvent as firewall\n" +
                "where intr.fromAddress = firewall.fromAddress and intr.toAddress = firewall.toAddress", path);

            env.compileDeploy("create table MyWindowTable (theWindow window(*) @type(SupportMySortValueEvent))", path);
            env.compileDeploy("select theWindow.first(), theWindow.last(), theWindow.window() from SupportMySortValueEvent, MyWindowTable", path);

            env.undeployAll();
        }
    }

}
