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
package com.espertech.esper.regression.resultset;

import com.espertech.esper.client.*;
import com.espertech.esper.client.deploy.DeploymentResult;
import com.espertech.esper.client.hook.AggregationFunctionFactory;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.time.CurrentTimeEvent;
import com.espertech.esper.epl.agg.access.AggregationAccessor;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.access.AggregationStateKey;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.AggregationValidationContext;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.rettype.EPType;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.plugin.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;
import junit.framework.TestCase;

import java.util.Collection;

public class TestAggregateFilterNamedParameter extends TestCase {

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp() {
        listener = new SupportUpdateListener();
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getEngineDefaults().getExecution().setAllowIsolatedService(true);
        config.getEngineDefaults().getViewResources().setShareViews(false);
        config.addEventType(SupportBean.class);
        config.addEventType(SupportBean_S0.class);
        config.addEventType(SupportBean_S1.class);
        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.startTest(epService, this.getClass(), getName());
        }
    }

    protected void tearDown() throws Exception {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.endTest();
        }
        listener = null;
    }

    public void testWFilter() throws Exception {
        runAssertionFirstAggSODA(false);
        runAssertionFirstAggSODA(true);

        runAssertionMethodAggSQLAll();
        runAssertionMethodAggSQLMixedFilter();
        runAssertionMethodAggLeaving();
        runAssertionMethodAggNth();
        runAssertionMethodAggRateUnbound();
        runAssertionMethodAggRateBound();
        runAssertionMethodPlugIn();

        runAssertionAccessAggLinearBound(false);
        runAssertionAccessAggLinearBound(true);
        runAssertionAccessAggLinearUnbound(false);
        runAssertionAccessAggLinearUnbound(true);
        runAssertionAccessAggLinearWIndex();
        runAssertionAccessAggLinearBoundMixedFilter();
        runAssertionAccessAggPlugIn();

        runAssertionAccessAggSortedBound(false);
        runAssertionAccessAggSortedBound(true);
        runAssertionAccessAggSortedUnbound(false);
        runAssertionAccessAggSortedUnbound(true);
        runAssertionAccessAggSortedMulticriteria();

        runAssertionIntoTable(false);
        runAssertionIntoTable(true);
        runAssertionIntoTableCountMinSketch();

        runAssertionAuditAndReuse();
    }

    private void runAssertionAccessAggPlugIn() {
        ConfigurationPlugInAggregationMultiFunction config = new ConfigurationPlugInAggregationMultiFunction();
        config.setFunctionNames("concatAccessAgg".split(","));
        config.setMultiFunctionFactoryClassName(MyAccessAggFactory.class.getName());
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationMultiFunction(config);

        String[] fields = "c0".split(",");
        String epl = "select concatAccessAgg(theString, filter:theString like 'A%') as c0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventAssert("X1", 0, fields, new Object[] {""});
        sendEventAssert("A1", 0, fields, new Object[] {"A1"});
        sendEventAssert("A2", 0, fields, new Object[] {"A1A2"});
        sendEventAssert("X2", 0, fields, new Object[] {"A1A2"});

        stmt.destroy();
    }

    private void runAssertionMethodPlugIn() {
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("concatMethodAgg", MyMethodAggFuncFactory.class.getName());

        String[] fields = "c0".split(",");
        String epl = "select concatMethodAgg(theString, filter:theString like 'A%') as c0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventAssert("X1", 0, fields, new Object[] {""});
        sendEventAssert("A1", 0, fields, new Object[] {"A1"});
        sendEventAssert("A2", 0, fields, new Object[] {"A1A2"});
        sendEventAssert("X2", 0, fields, new Object[] {"A1A2"});

        stmt.destroy();
    }

    private void runAssertionIntoTableCountMinSketch() throws Exception {
        String epl =
                "create table WordCountTable(wordcms countMinSketch());\n" +
                "into table WordCountTable select countMinSketchAdd(theString, filter:intPrimitive > 0) as wordcms from SupportBean;\n" +
                "@name('stmt') select WordCountTable.wordcms.countMinSketchFrequency(p00) as c0 from SupportBean_S0;\n";

        DeploymentResult deploymentResult = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPAdministrator().getStatement("stmt").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));

        sendEvent("hello", 0);
        sendEventAssertCount("hello", 0L);

        sendEvent("name", 1);
        sendEventAssertCount("name", 1L);

        sendEvent("name", 0);
        sendEventAssertCount("name", 1L);

        sendEvent("name", 1);
        sendEventAssertCount("name", 2L);

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentResult.getDeploymentId());
    }

    private void runAssertionMethodAggRateBound() {
        String[] fields = "myrate,myqtyrate".split(",");
        String epl = "select " +
                "rate(longPrimitive, filter:theString like 'A%') as myrate, " +
                "rate(longPrimitive, intPrimitive, filter:theString like 'A%') as myqtyrate " +
                "from SupportBean#length(3)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventWLong("X1", 1000, 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        sendEventWLong("X2", 1200, 0);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        sendEventWLong("X2", 1300, 0);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        sendEventWLong("A1", 1000, 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        sendEventWLong("A2", 1200, 0);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        sendEventWLong("A3", 1300, 0);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null});

        sendEventWLong("A4", 1500, 14);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3 * 1000 / 500d, 14 * 1000 / 500d});

        sendEventWLong("A5", 2000, 11);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{3 * 1000 / 800d, 25 * 1000 / 800d});

        stmt.destroy();
    }

    private void runAssertionMethodAggRateUnbound() {

        EPServiceProviderIsolated isolated = epService.getEPServiceIsolated("I1");
        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(0));

        String[] fields = "c0".split(",");
        String epl = "select rate(1, filter:theString like 'A%') as c0 from SupportBean";
        EPStatement stmt = isolated.getEPAdministrator().createEPL(epl, "stmt1", null);
        stmt.addListener(listener);

        sendEventAssert(isolated, "X1", 0, fields, new Object[]{null});
        sendEventAssert(isolated, "A1", 1, fields, new Object[]{null});

        isolated.getEPRuntime().sendEvent(new CurrentTimeEvent(1000));
        sendEventAssert(isolated, "X2", 2, fields, new Object[]{null});
        sendEventAssert(isolated, "A2", 2, fields, new Object[]{1.0});
        sendEventAssert(isolated, "A3", 3, fields, new Object[]{2.0});

        stmt.destroy();
        isolated.destroy();
    }

    private void runAssertionMethodAggNth() {
        String[] fields = "c0".split(",");
        String epl = "select nth(intPrimitive, 1, filter:theString like 'A%') as c0 from SupportBean";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventAssert("X1", 0, fields, new Object[]{null});
        sendEventAssert("X2", 0, fields, new Object[]{null});
        sendEventAssert("A3", 1, fields, new Object[]{null});
        sendEventAssert("A4", 2, fields, new Object[]{1});
        sendEventAssert("X3", 0, fields, new Object[]{1});
        sendEventAssert("A5", 3, fields, new Object[]{2});
        sendEventAssert("X4", 0, fields, new Object[]{2});

        stmt.destroy();
    }

    private void runAssertionMethodAggLeaving() {
        String[] fields = "c0,c1".split(",");
        String epl = "select " +
                "leaving(filter:intPrimitive=1) as c0," +
                "leaving(filter:intPrimitive=2) as c1" +
                " from SupportBean#length(2)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventAssert("E1", 2, fields, new Object[]{false, false});
        sendEventAssert("E2", 1, fields, new Object[]{false, false});
        sendEventAssert("E3", 3, fields, new Object[]{false, true});
        sendEventAssert("E4", 4, fields, new Object[]{true, true});

        stmt.destroy();
    }

    private void runAssertionAuditAndReuse() {
        String epl = "select " +
                "sum(intPrimitive, filter:intPrimitive=1) as c0, sum(intPrimitive, filter:intPrimitive=1) as c1, " +
                "window(*, filter:intPrimitive=1) as c2, window(*, filter:intPrimitive=1) as c3 " +
                " from SupportBean#length(3)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 0));

        stmt.destroy();
    }

    public void testInvalid() {

        // invalid filter expression name parameter: multiple values
        SupportMessageAssertUtil.tryInvalid(epService, "select sum(intPrimitive, filter:(intPrimitive, doublePrimitive)) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'sum(intPrimitive,filter:(intPrimiti...(55 chars)': Filter named parameter requires a single expression returning a boolean-typed value");

        // multiple filter expressions
        SupportMessageAssertUtil.tryInvalid(epService, "select sum(intPrimitive, intPrimitive > 0, filter:intPrimitive < 0) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'sum(intPrimitive,intPrimitive>0,fil...(54 chars)': Only a single filter expression can be provided");

        // invalid filter expression name parameter: not returning boolean
        SupportMessageAssertUtil.tryInvalid(epService, "select sum(intPrimitive, filter:intPrimitive) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'sum(intPrimitive,filter:intPrimitive)': Filter named parameter requires a single expression returning a boolean-typed value");

        // create-table does not allow filters
        SupportMessageAssertUtil.tryInvalid(epService, "create table MyTable(totals sum(int, filter:true))",
                "Error starting statement: Failed to validate table-column expression 'sum(int,filter:true)': The 'group_by' and 'filter' parameter is not allowed in create-table statements");

        // invalid correlated subquery
        SupportMessageAssertUtil.tryInvalid(epService, "select (select sum(intPrimitive, filter:s0.p00='a') from SupportBean) from SupportBean_S0 as s0",
                "Error starting statement: Failed to plan subquery number 1 querying SupportBean: Subselect aggregation functions cannot aggregate across correlated properties");

    }

    private void runAssertionIntoTable(boolean join) throws Exception {
        String epl =
                "create table MyTable(\n" +
                        "totalA sum(int, true),\n" +
                        "totalB sum(int, true),\n" +
                        "winA window(*) @type(SupportBean),\n" +
                        "winB window(*) @type(SupportBean),\n" +
                        "sortedA sorted(intPrimitive) @type(SupportBean),\n" +
                        "sortedB sorted(intPrimitive) @type(SupportBean));\n" +
                        "into table MyTable select\n" +
                        "sum(intPrimitive, filter: theString like 'A%') as totalA,\n" +
                        "sum(intPrimitive, filter: theString like 'B%') as totalB,\n" +
                        "window(sb, filter: theString like 'A%') as winA,\n" +
                        "window(sb, filter: theString like 'B%') as winB,\n" +
                        "sorted(sb, filter: theString like 'A%') as sortedA,\n" +
                        "sorted(sb, filter: theString like 'B%') as sortedB\n" +
                        "from " + (join ? "SupportBean_S1#lastevent, SupportBean#keepall as sb;\n" : "SupportBean as sb;\n") +
                        "@name('stmt') select MyTable.totalA as ta, MyTable.totalB as tb, MyTable.winA as wa, MyTable.winB as wb, MyTable.sortedA as sa, MyTable.sortedB as sb from SupportBean_S0";
        DeploymentResult deploymentResult = epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(epl);
        epService.getEPAdministrator().getStatement("stmt").addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));

        sendEvent("X1", 1);
        sendEventAssertInfoTable(null, null, null, null, null, null);

        SupportBean a1 = sendEvent("A1", 1);
        sendEventAssertInfoTable(1, null, new SupportBean[]{a1}, null, new SupportBean[]{a1}, null);

        SupportBean b2 = sendEvent("B2", 20);
        sendEventAssertInfoTable(1, 20, new SupportBean[]{a1}, new SupportBean[]{b2}, new SupportBean[]{a1}, new SupportBean[]{b2});

        SupportBean a3 = sendEvent("A3", 10);
        sendEventAssertInfoTable(11, 20, new SupportBean[]{a1, a3}, new SupportBean[]{b2}, new SupportBean[]{a1, a3}, new SupportBean[]{b2});

        SupportBean b4 = sendEvent("B4", 2);
        sendEventAssertInfoTable(11, 22, new SupportBean[]{a1, a3}, new SupportBean[]{b2, b4}, new SupportBean[]{a1, a3}, new SupportBean[]{b4, b2});

        epService.getEPAdministrator().getDeploymentAdmin().undeploy(deploymentResult.getDeploymentId());
    }

    private void runAssertionAccessAggLinearWIndex() {
        String[] fields = "c0,c1,c2,c3".split(",");
        String epl = "select " +
                "first(intPrimitive, 0, filter:theString like 'A%') as c0," +
                "first(intPrimitive, 1, filter:theString like 'A%') as c1," +
                "last(intPrimitive, 0, filter:theString like 'A%') as c2," +
                "last(intPrimitive, 1, filter:theString like 'A%') as c3" +
                " from SupportBean#length(3)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventAssert("B1", 1, fields, new Object[]{null, null, null, null});
        sendEventAssert("A2", 2, fields, new Object[]{2, null, 2, null});
        sendEventAssert("A3", 3, fields, new Object[]{2, 3, 3, 2});
        sendEventAssert("A4", 4, fields, new Object[]{2, 3, 4, 3});
        sendEventAssert("B2", 2, fields, new Object[]{3, 4, 4, 3});
        sendEventAssert("B3", 3, fields, new Object[]{4, null, 4, null});
        sendEventAssert("B4", 4, fields, new Object[]{null, null, null, null});

        stmt.destroy();
    }

    private void runAssertionAccessAggSortedBound(boolean join) {
        String[] fields = "aMaxby,aMinby,aSorted,bMaxby,bMinby,bSorted".split(",");
        String epl = "select " +
                "maxby(intPrimitive, filter:theString like 'A%').theString as aMaxby," +
                "minby(intPrimitive, filter:theString like 'A%').theString as aMinby," +
                "sorted(intPrimitive, filter:theString like 'A%') as aSorted," +
                "maxby(intPrimitive, filter:theString like 'B%').theString as bMaxby," +
                "minby(intPrimitive, filter:theString like 'B%').theString as bMinby," +
                "sorted(intPrimitive, filter:theString like 'B%') as bSorted" +
                " from " + (join ? "SupportBean_S1#lastevent, SupportBean#length(4)" : "SupportBean#length(4)");

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));

        SupportBean b1 = sendEvent("B1", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, "B1", "B1", new SupportBean[]{b1}});

        SupportBean a10 = sendEvent("A10", 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A10", "A10", new SupportBean[]{a10}, "B1", "B1", new SupportBean[]{b1}});

        SupportBean b2 = sendEvent("B2", 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A10", "A10", new SupportBean[]{a10}, "B2", "B1", new SupportBean[]{b1, b2}});

        SupportBean a5 = sendEvent("A5", 5);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A10", "A5", new SupportBean[]{a5, a10}, "B2", "B1", new SupportBean[]{b1, b2}});

        SupportBean a15 = sendEvent("A15", 15);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A15", "A5", new SupportBean[]{a5, a10, a15}, "B2", "B2", new SupportBean[]{b2}});

        sendEvent("X3", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A15", "A5", new SupportBean[]{a5, a15}, "B2", "B2", new SupportBean[]{b2}});

        sendEvent("X4", 4);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A15", "A5", new SupportBean[]{a5, a15}, null, null, null});

        sendEvent("X5", 5);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A15", "A15", new SupportBean[]{a15}, null, null, null});

        sendEvent("X6", 6);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, null, null, null, null, null});

        stmt.destroy();
    }

    private void runAssertionAccessAggSortedMulticriteria() {
        String[] fields = "aSorted,bSorted".split(",");
        String epl = "select " +
                "sorted(intPrimitive, doublePrimitive, filter:theString like 'A%') as aSorted," +
                "sorted(intPrimitive, doublePrimitive, filter:theString like 'B%') as bSorted" +
                " from SupportBean#keepall";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        SupportBean b1 = sendEvent("B1", 1, 10);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, new SupportBean[]{b1}});

        SupportBean a1 = sendEvent("A1", 100, 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{new SupportBean[]{a1}, new SupportBean[]{b1}});

        SupportBean b2 = sendEvent("B2", 1, 4);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{new SupportBean[]{a1}, new SupportBean[]{b2, b1}});

        SupportBean a2 = sendEvent("A2", 100, 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{new SupportBean[]{a1, a2}, new SupportBean[]{b2, b1}});

        stmt.destroy();
    }

    private void runAssertionAccessAggSortedUnbound(boolean join) {
        String[] fields = "aMaxby,aMaxbyever,aMinby,aMinbyever".split(",");
        String epl = "select " +
                "maxby(intPrimitive, filter:theString like 'A%').theString as aMaxby," +
                "maxbyever(intPrimitive, filter:theString like 'A%').theString as aMaxbyever," +
                "minby(intPrimitive, filter:theString like 'A%').theString as aMinby," +
                "minbyever(intPrimitive, filter:theString like 'A%').theString as aMinbyever" +
                " from " + (join ? "SupportBean_S1#lastevent, SupportBean#keepall" : "SupportBean");

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));

        sendEventAssert("B1", 1, fields, new Object[]{null, null, null, null});
        sendEventAssert("A10", 10, fields, new Object[]{"A10", "A10", "A10", "A10"});
        sendEventAssert("A5", 5, fields, new Object[]{"A10", "A10", "A5", "A5"});
        sendEventAssert("A15", 15, fields, new Object[]{"A15", "A15", "A5", "A5"});
        sendEventAssert("B1000", 1000, fields, new Object[]{"A15", "A15", "A5", "A5"});

        stmt.destroy();
    }

    private void runAssertionAccessAggLinearBound(boolean join) {
        String[] fields = "aFirst,aLast,aWindow,bFirst,bLast,bWindow".split(",");
        String epl = "select " +
                "first(intPrimitive, filter:theString like 'A%') as aFirst," +
                "last(intPrimitive, filter:theString like 'A%') as aLast," +
                "window(intPrimitive, filter:theString like 'A%') as aWindow," +
                "first(intPrimitive, filter:theString like 'B%') as bFirst," +
                "last(intPrimitive, filter:theString like 'B%') as bLast," +
                "window(intPrimitive, filter:theString like 'B%') as bWindow" +
                " from " + (join ? "SupportBean_S1#lastevent, SupportBean#length(5)" : "SupportBean#length(5)");

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));

        sendEventAssert("X1", 1, fields, new Object[]{null, null, null, null, null, null});
        sendEventAssert("B2", 2, fields, new Object[]{null, null, null, 2, 2, new int[]{2}});
        sendEventAssert("B3", 3, fields, new Object[]{null, null, null, 2, 3, new int[]{2, 3}});
        sendEventAssert("A4", 4, fields, new Object[]{4, 4, new int[]{4}, 2, 3, new int[]{2, 3}});
        sendEventAssert("B5", 5, fields, new Object[]{4, 4, new int[]{4}, 2, 5, new int[]{2, 3, 5}});
        sendEventAssert("A6", 6, fields, new Object[]{4, 6, new int[]{4, 6}, 2, 5, new int[]{2, 3, 5}});
        sendEventAssert("X2", 7, fields, new Object[]{4, 6, new int[]{4, 6}, 3, 5, new int[]{3, 5}});
        sendEventAssert("X3", 8, fields, new Object[]{4, 6, new int[]{4, 6}, 5, 5, new int[]{5}});
        sendEventAssert("X4", 9, fields, new Object[]{6, 6, new int[]{6}, 5, 5, new int[]{5}});
        sendEventAssert("X5", 10, fields, new Object[]{6, 6, new int[]{6}, null, null, null});
        sendEventAssert("X6", 11, fields, new Object[]{null, null, null, null, null, null});

        stmt.destroy();
    }

    private void runAssertionAccessAggLinearUnbound(boolean join) {
        String[] fields = "aFirst,aFirstever,aLast,aLastever,aCountever".split(",");
        String epl = "select " +
                "first(intPrimitive, filter:theString like 'A%') as aFirst," +
                "firstever(intPrimitive, filter:theString like 'A%') as aFirstever," +
                "last(intPrimitive, filter:theString like 'A%') as aLast," +
                "lastever(intPrimitive, filter:theString like 'A%') as aLastever," +
                "countever(intPrimitive, filter:theString like 'A%') as aCountever" +
                " from " + (join ? "SupportBean_S1#lastevent, SupportBean#keepall" : "SupportBean");

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean_S1(0));

        sendEventAssert("X0", 0, fields, new Object[]{null, null, null, null, 0L});
        sendEventAssert("A1", 1, fields, new Object[]{1, 1, 1, 1, 1L});
        sendEventAssert("X2", 2, fields, new Object[]{1, 1, 1, 1, 1L});
        sendEventAssert("A3", 3, fields, new Object[]{1, 1, 3, 3, 2L});
        sendEventAssert("X4", 4, fields, new Object[]{1, 1, 3, 3, 2L});

        stmt.destroy();
    }

    private void runAssertionAccessAggLinearBoundMixedFilter() {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "window(sb, filter:theString like 'A%') as c0," +
                "window(sb) as c1," +
                "window(filter:theString like 'B%', sb) as c2" +
                " from SupportBean#keepall as sb";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        SupportBean x1 = sendEvent("X1", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, new SupportBean[]{x1}, null});

        SupportBean a2 = sendEvent("A2", 2);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{new SupportBean[]{a2}, new SupportBean[]{x1, a2}, null});

        SupportBean b3 = sendEvent("B3", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{new SupportBean[]{a2}, new SupportBean[]{x1, a2, b3}, new SupportBean[]{b3}});

        stmt.destroy();
    }

    private void runAssertionMethodAggSQLMixedFilter() {
        String[] fields = "c0,c1,c2".split(",");
        String epl = "select " +
                "sum(intPrimitive, filter:theString like 'A%') as c0," +
                "sum(intPrimitive) as c1," +
                "sum(filter:theString like 'B%', intPrimitive) as c2" +
                " from SupportBean";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventAssert("X1", 1, fields, new Object[]{null, 1, null});
        sendEventAssert("B2", 20, fields, new Object[]{null, 1 + 20, 20});
        sendEventAssert("A3", 300, fields, new Object[]{300, 1 + 20 + 300, 20});
        sendEventAssert("X1", 2, fields, new Object[]{300, 1 + 20 + 300 + 2, 20});

        stmt.destroy();
    }

    private void runAssertionMethodAggSQLAll() {
        String epl = "select " +
                "avedev(doublePrimitive, filter:intPrimitive between 1 and 3) as cAvedev," +
                "avg(doublePrimitive, filter:intPrimitive between 1 and 3) as cAvg, " +
                "count(*, filter:intPrimitive between 1 and 3) as cCount, " +
                "max(doublePrimitive, filter:intPrimitive between 1 and 3) as cMax, " +
                "fmax(doublePrimitive, filter:intPrimitive between 1 and 3) as cFmax, " +
                "maxever(doublePrimitive, filter:intPrimitive between 1 and 3) as cMaxever, " +
                "fmaxever(doublePrimitive, filter:intPrimitive between 1 and 3) as cFmaxever, " +
                "median(doublePrimitive, filter:intPrimitive between 1 and 3) as cMedian, " +
                "min(doublePrimitive, filter:intPrimitive between 1 and 3) as cMin, " +
                "fmin(doublePrimitive, filter:intPrimitive between 1 and 3) as cFmin, " +
                "minever(doublePrimitive, filter:intPrimitive between 1 and 3) as cMinever, " +
                "fminever(doublePrimitive, filter:intPrimitive between 1 and 3) as cFminever, " +
                "stddev(doublePrimitive, filter:intPrimitive between 1 and 3) as cStddev, " +
                "sum(doublePrimitive, filter:intPrimitive between 1 and 3) as cSum " +
                "from SupportBean";

        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendEventAssertSQLFuncs("E1", 0, 50, null, null, 0L, null, null, null, null, null, null, null, null, null, null, null);
        sendEventAssertSQLFuncs("E2", 2, 10, 0.0, 10d, 1L, 10d, 10d, 10d, 10d, 10.0, 10d, 10d, 10d, 10d, null, 10d);
        sendEventAssertSQLFuncs("E3", 100, 10, 0.0, 10d, 1L, 10d, 10d, 10d, 10d, 10.0, 10d, 10d, 10d, 10d, null, 10d);
        sendEventAssertSQLFuncs("E4", 1, 20, 5.0, 15d, 2L, 20d, 20d, 20d, 20d, 15.0, 10d, 10d, 10d, 10d, 7.0710678118654755, 30d);

        stmt.destroy();
    }

    private void runAssertionFirstAggSODA(boolean soda) {
        String[] fields = "c0,c1".split(",");
        String epl = "select " +
                "first(*,filter:intPrimitive=1).theString as c0, " +
                "first(*,filter:intPrimitive=2).theString as c1" +
                " from SupportBean#length(3)";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        stmt.addListener(listener);

        sendEventAssert("E1", 3, fields, new Object[]{null, null});
        sendEventAssert("E2", 2, fields, new Object[]{null, "E2"});
        sendEventAssert("E3", 1, fields, new Object[]{"E3", "E2"});
        sendEventAssert("E4", 2, fields, new Object[]{"E3", "E2"});
        sendEventAssert("E5", -1, fields, new Object[]{"E3", "E4"});
        sendEventAssert("E6", -1, fields, new Object[]{null, "E4"});
        sendEventAssert("E7", -1, fields, new Object[]{null, null});

        stmt.destroy();
    }

    private void sendEventAssertSQLFuncs(String theString, int intPrimitive, double doublePrimitive,
                                         Object cAvedev, Object cAvg, Object cCount,
                                         Object cMax, Object cFmax, Object cMaxever, Object cFmaxever,
                                         Object cMedian,
                                         Object cMin, Object cFmin, Object cMinever, Object cFminever,
                                         Object cStddev, Object cSum) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        epService.getEPRuntime().sendEvent(sb);
        EventBean event = listener.assertOneGetNewAndReset();
        assertEquals(cAvedev, event.get("cAvedev"));
        assertEquals(cAvg, event.get("cAvg"));
        assertEquals(cCount, event.get("cCount"));
        assertEquals(cMax, event.get("cMax"));
        assertEquals(cFmax, event.get("cFmax"));
        assertEquals(cMaxever, event.get("cMaxever"));
        assertEquals(cFmaxever, event.get("cFmaxever"));
        assertEquals(cMedian, event.get("cMedian"));
        assertEquals(cMin, event.get("cMin"));
        assertEquals(cFmin, event.get("cFmin"));
        assertEquals(cMinever, event.get("cMinever"));
        assertEquals(cFminever, event.get("cFminever"));
        assertEquals(cStddev, event.get("cStddev"));
        assertEquals(cSum, event.get("cSum"));
    }

    private void sendEventAssert(String theString, int intPrimitive, String[] fields, Object[] expected) {
        sendEvent(theString, intPrimitive);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);
    }

    private void sendEventAssert(EPServiceProviderIsolated isolated, String theString, int intPrimitive, String[] fields, Object[] expected) {
        isolated.getEPRuntime().sendEvent(new SupportBean(theString, intPrimitive));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, expected);
    }

    private SupportBean sendEvent(String theString, int intPrimitive) {
        return sendEvent(theString, intPrimitive, -1);
    }

    private SupportBean sendEvent(String theString, int intPrimitive, double doublePrimitive) {
        SupportBean sb = new SupportBean(theString, intPrimitive);
        sb.setDoublePrimitive(doublePrimitive);
        epService.getEPRuntime().sendEvent(sb);
        return sb;
    }

    private void sendEventAssertInfoTable(Object ta, Object tb, Object wa, Object wb, Object sa, Object sb) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "ta,tb,wa,wb,sa,sb".split(","), new Object[]{ta, tb, wa, wb, sa, sb});
    }

    private void sendEventAssertCount(String p00, Object expected) {
        epService.getEPRuntime().sendEvent(new SupportBean_S0(0, p00));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0".split(","), new Object[]{expected});
    }

    private void sendEventWLong(String theString, long longPrimitive, int intPrimitive) {
        SupportBean bean = new SupportBean(theString, intPrimitive);
        bean.setLongPrimitive(longPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    public static class MyMethodAggFuncFactory implements AggregationFunctionFactory {
        public void setFunctionName(String functionName) {
        }

        public void validate(AggregationValidationContext validationContext) {
            assertNotNull(validationContext.getNamedParameters().get("filter").iterator().next());
        }

        public AggregationMethod newAggregator() {
            return new MyMethodAggMethod();
        }

        public Class getValueType() {
            return String.class;
        }
    }

    public static class MyMethodAggMethod implements AggregationMethod {
        StringBuffer buffer = new StringBuffer();
        public void enter(Object value) {
            Object[] arr = (Object[]) value;
            Boolean pass = (Boolean) arr[1];
            if (pass != null && pass) {
                buffer.append(arr[0].toString());
            }
        }

        public void leave(Object value) {
            // not implemented
        }

        public Object getValue() {
            return buffer.toString();
        }

        public void clear() {
            buffer = new StringBuffer();
        }
    }

    public static class MyAccessAggFactory implements PlugInAggregationMultiFunctionFactory {
        public void addAggregationFunction(PlugInAggregationMultiFunctionDeclarationContext declarationContext) {

        }

        public PlugInAggregationMultiFunctionHandler validateGetHandler(PlugInAggregationMultiFunctionValidationContext validationContext) {
            assertNotNull(validationContext.getNamedParameters().get("filter").iterator().next());
            ExprEvaluator valueEval = validationContext.getParameterExpressions()[0].getExprEvaluator();
            ExprEvaluator filterEval = validationContext.getNamedParameters().get("filter").get(0).getExprEvaluator();
            return new MyAccessAggHandler(valueEval, filterEval);
        }
    }

    public static class MyAccessAggHandler implements PlugInAggregationMultiFunctionHandler {

        private final ExprEvaluator valueEval;
        private final ExprEvaluator filterEval;

        public MyAccessAggHandler(ExprEvaluator valueEval, ExprEvaluator filterEval) {
            this.valueEval = valueEval;
            this.filterEval = filterEval;
        }

        public AggregationAccessor getAccessor() {
            return new AggregationAccessor() {
                public Object getValue(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    return ((MyAccessAggState) state).getBuffer().toString();
                }

                public Collection<EventBean> getEnumerableEvents(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    return null;
                }

                public EventBean getEnumerableEvent(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    return null;
                }

                public Collection<Object> getEnumerableScalar(AggregationState state, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
                    return null;
                }
            };
        }

        public EPType getReturnType() {
            return EPTypeHelper.singleValue(String.class);
        }

        public AggregationStateKey getAggregationStateUniqueKey() {
            return new AggregationStateKey(){};
        }

        public PlugInAggregationMultiFunctionStateFactory getStateFactory() {
            return new PlugInAggregationMultiFunctionStateFactory() {
                public AggregationState makeAggregationState(PlugInAggregationMultiFunctionStateContext stateContext) {
                    return new MyAccessAggState(valueEval, filterEval);
                }
            };
        }

        public AggregationAgent getAggregationAgent(PlugInAggregationMultiFunctionAgentContext agentContext) {
            return null;
        }
    }

    private static class MyAccessAggState implements AggregationState {
        private final ExprEvaluator valueEval;
        private final ExprEvaluator filterEval;

        private StringBuffer buffer = new StringBuffer();

        public MyAccessAggState(ExprEvaluator valueEval, ExprEvaluator filterEval) {
            this.valueEval = valueEval;
            this.filterEval = filterEval;
        }

        public StringBuffer getBuffer() {
            return buffer;
        }

        public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
            Boolean pass = (Boolean) filterEval.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if (pass != null && pass) {
                Object value = valueEval.evaluate(eventsPerStream, true, exprEvaluatorContext);
                buffer.append(value);
            }
        }

        public void applyLeave(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
            // no need
        }

        public void clear() {
            buffer = new StringBuffer();
        }
    }
}
