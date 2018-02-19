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
package com.espertech.esper.regression.epl.fromclausemethod;

import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.core.service.StatementType;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.epl.SupportMethodInvocationJoinInvalid;
import com.espertech.esper.supportregression.epl.SupportStaticMethodLib;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.supportregression.util.SupportModelHelper;

import java.util.Collections;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecFromClauseMethod implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        runAssertionUDFAndScriptReturningEvents(epService);
        runAssertionEventBeanArray(epService);
        runAssertionOverloaded(epService);
        runAssertion2StreamMaxAggregation(epService);
        runAssertion2JoinHistoricalSubordinateOuterMultiField(epService);
        runAssertion2JoinHistoricalSubordinateOuter(epService);
        runAssertion2JoinHistoricalIndependentOuter(epService);
        runAssertion2JoinHistoricalOnlyDependent(epService);
        runAssertion2JoinHistoricalOnlyIndependent(epService);
        runAssertionNoJoinIterateVariables(epService);
        runAssertionDifferentReturnTypes(epService);
        runAssertionArrayNoArg(epService);
        runAssertionArrayWithArg(epService);
        runAssertionObjectNoArg(epService);
        runAssertionObjectWithArg(epService);
        runAssertionInvocationTargetEx(epService);
        runAssertionStreamNameWContext(epService);
        runAssertionWithMethodResultParam(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionWithMethodResultParam(EPServiceProvider epService) {
        String epl = "@name('stmt0') select * from SupportBean as e,\n" +
                "method:" + this.getClass().getName() + ".getWithMethodResultParam('somevalue', e, "
                + this.getClass().getName() + ".getWithMethodResultParamCompute(true)) as s";
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "s.p00,s.p01,s.p02".split(","), new Object[] {"somevalue", "E1", "stmt0"});

        stmt.destroy();
    }

    private void runAssertionStreamNameWContext(EPServiceProvider epService) {
        String epl = "@name('stmt0') select * from SupportBean as e,\n" +
                "method:" + this.getClass().getName() + ".getStreamNameWContext('somevalue', e) as s";
        SupportUpdateListener listener = new SupportUpdateListener();
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "s.p00,s.p01,s.p02".split(","), new Object[] {"somevalue", "E1", "stmt0"});

        stmt.destroy();
    }

    private void runAssertionUDFAndScriptReturningEvents(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().createEPL("create schema ItemEvent(id string)");

        ConfigurationPlugInSingleRowFunction entry = new ConfigurationPlugInSingleRowFunction();
        entry.setName("myItemProducerUDF");
        entry.setFunctionClassName(this.getClass().getName());
        entry.setFunctionMethodName("myItemProducerUDF");
        entry.setEventTypeName("ItemEvent");
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction(entry);

        String script = "create expression EventBean[] @type(ItemEvent) js:myItemProducerScript() [\n" +
                "myItemProducerScript();" +
                "function myItemProducerScript() {" +
                "  var EventBeanArray = Java.type(\"com.espertech.esper.client.EventBean[]\");\n" +
                "  var events = new EventBeanArray(2);\n" +
                "  events[0] = epl.getEventBeanService().adapterForMap(java.util.Collections.singletonMap(\"id\", \"id1\"), \"ItemEvent\");\n" +
                "  events[1] = epl.getEventBeanService().adapterForMap(java.util.Collections.singletonMap(\"id\", \"id3\"), \"ItemEvent\");\n" +
                "  return events;\n" +
                "}]";
        epService.getEPAdministrator().getDeploymentAdmin().parseDeploy(script);

        tryAssertionUDFAndScriptReturningEvents(epService, "myItemProducerUDF");
        tryAssertionUDFAndScriptReturningEvents(epService, "myItemProducerScript");

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionEventBeanArray(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create schema MyItemEvent(p0 string)");

        tryAssertionEventBeanArray(epService, "eventBeanArrayForString", false);
        tryAssertionEventBeanArray(epService, "eventBeanArrayForString", true);
        tryAssertionEventBeanArray(epService, "eventBeanCollectionForString", false);
        tryAssertionEventBeanArray(epService, "eventBeanIteratorForString", false);

        SupportMessageAssertUtil.tryInvalid(epService, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".fetchResult12(0) @type(ItemEvent)",
                "Error starting statement: The @type annotation is only allowed when the invocation target returns EventBean instances");
    }

    private void runAssertionOverloaded(EPServiceProvider epService) {
        tryAssertionOverloaded(epService, "", "A", "B");
        tryAssertionOverloaded(epService, "10", "10", "B");
        tryAssertionOverloaded(epService, "10, 20", "10", "20");
        tryAssertionOverloaded(epService, "'x'", "x", "B");
        tryAssertionOverloaded(epService, "'x', 50", "x", "50");
    }

    private void tryAssertionOverloaded(EPServiceProvider epService, String params, String expectedFirst, String expectedSecond) {
        String epl = "select col1, col2 from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".overloadedMethodForJoin(" + params + ")";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "col1,col2".split(","), new Object[]{expectedFirst, expectedSecond});
        stmt.destroy();
    }

    private void runAssertion2StreamMaxAggregation(EPServiceProvider epService) {
        String className = SupportStaticMethodLib.class.getName();
        String stmtText;

        // ESPER 556
        stmtText = "select max(col1) as maxcol1 from SupportBean#unique(theString), method:" + className + ".fetchResult100() ";

        String[] fields = "maxcol1".split(",");
        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        assertFalse(stmt.getStatementContext().isStatelessSelect());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{9}});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{9}});

        stmt.destroy();
    }

    private void runAssertion2JoinHistoricalSubordinateOuterMultiField(EPServiceProvider epService) {
        String className = SupportStaticMethodLib.class.getName();
        String stmtText;

        // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
        stmtText = "select intPrimitive,intBoxed,col1,col2 from SupportBean#keepall " +
                "left outer join " +
                "method:" + className + ".fetchResult100() " +
                "on intPrimitive = col1 and intBoxed = col2";

        String[] fields = "intPrimitive,intBoxed,col1,col2".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendSupportBeanEvent(epService, 2, 4);
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), fields, new Object[][]{{2, 4, 2, 4}});
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{2, 4, 2, 4}});

        stmt.destroy();
    }

    private void runAssertion2JoinHistoricalSubordinateOuter(EPServiceProvider epService) {
        String className = SupportStaticMethodLib.class.getName();
        String stmtText;

        // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
        stmtText = "select s0.value as valueOne, s1.value as valueTwo from method:" + className + ".fetchResult12(0) as s0 " +
                "left outer join " +
                "method:" + className + ".fetchResult23(s0.value) as s1 on s0.value = s1.value";
        assertJoinHistoricalSubordinateOuter(epService, stmtText);

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchResult23(s0.value) as s1 " +
                "right outer join " +
                "method:" + className + ".fetchResult12(0) as s0 on s0.value = s1.value";
        assertJoinHistoricalSubordinateOuter(epService, stmtText);

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchResult23(s0.value) as s1 " +
                "full outer join " +
                "method:" + className + ".fetchResult12(0) as s0 on s0.value = s1.value";
        assertJoinHistoricalSubordinateOuter(epService, stmtText);

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchResult12(0) as s0 " +
                "full outer join " +
                "method:" + className + ".fetchResult23(s0.value) as s1 on s0.value = s1.value";
        assertJoinHistoricalSubordinateOuter(epService, stmtText);
    }

    private void runAssertion2JoinHistoricalIndependentOuter(EPServiceProvider epService) {
        String[] fields = "valueOne,valueTwo".split(",");
        String className = SupportStaticMethodLib.class.getName();
        String stmtText;

        // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
        stmtText = "select s0.value as valueOne, s1.value as valueTwo from method:" + className + ".fetchResult12(0) as s0 " +
                "left outer join " +
                "method:" + className + ".fetchResult23(0) as s1 on s0.value = s1.value";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, null}, {2, 2}});
        stmt.destroy();

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchResult23(0) as s1 " +
                "right outer join " +
                "method:" + className + ".fetchResult12(0) as s0 on s0.value = s1.value";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, null}, {2, 2}});
        stmt.destroy();

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchResult23(0) as s1 " +
                "full outer join " +
                "method:" + className + ".fetchResult12(0) as s0 on s0.value = s1.value";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, null}, {2, 2}, {null, 3}});
        stmt.destroy();

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchResult12(0) as s0 " +
                "full outer join " +
                "method:" + className + ".fetchResult23(0) as s1 on s0.value = s1.value";
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, null}, {2, 2}, {null, 3}});
        stmt.destroy();
    }

    private void assertJoinHistoricalSubordinateOuter(EPServiceProvider epService, String expression) {
        String[] fields = "valueOne,valueTwo".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, null}, {2, 2}});
        stmt.destroy();
    }

    private void runAssertion2JoinHistoricalOnlyDependent(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("create variable int lower");
        epService.getEPAdministrator().createEPL("create variable int upper");
        EPStatement setStmt = epService.getEPAdministrator().createEPL("on SupportBean set lower=intPrimitive,upper=intBoxed");
        assertEquals(StatementType.ON_SET, ((EPStatementSPI) setStmt).getStatementMetadata().getStatementType());

        String className = SupportStaticMethodLib.class.getName();
        String stmtText;

        // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
        stmtText = "select value,result from method:" + className + ".fetchBetween(lower, upper), " +
                "method:" + className + ".fetchIdDelimited(value)";
        assertJoinHistoricalOnlyDependent(epService, stmtText);

        stmtText = "select value,result from " +
                "method:" + className + ".fetchIdDelimited(value), " +
                "method:" + className + ".fetchBetween(lower, upper)";
        assertJoinHistoricalOnlyDependent(epService, stmtText);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertion2JoinHistoricalOnlyIndependent(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("create variable int lower");
        epService.getEPAdministrator().createEPL("create variable int upper");
        epService.getEPAdministrator().createEPL("on SupportBean set lower=intPrimitive,upper=intBoxed");

        String className = SupportStaticMethodLib.class.getName();
        String stmtText;

        // fetchBetween must execute first, fetchIdDelimited is dependent on the result of fetchBetween
        stmtText = "select s0.value as valueOne, s1.value as valueTwo from method:" + className + ".fetchBetween(lower, upper) as s0, " +
                "method:" + className + ".fetchBetweenString(lower, upper) as s1";
        assertJoinHistoricalOnlyIndependent(epService, stmtText);

        stmtText = "select s0.value as valueOne, s1.value as valueTwo from " +
                "method:" + className + ".fetchBetweenString(lower, upper) as s1, " +
                "method:" + className + ".fetchBetween(lower, upper) as s0 ";
        assertJoinHistoricalOnlyIndependent(epService, stmtText);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertJoinHistoricalOnlyIndependent(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "valueOne,valueTwo".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendSupportBeanEvent(epService, 5, 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{5, "5"}});

        sendSupportBeanEvent(epService, 1, 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, "1"}, {1, "2"}, {2, "1"}, {2, "2"}});

        sendSupportBeanEvent(epService, 0, -1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        stmt.destroy();
        sendSupportBeanEvent(epService, 0, -1);
        assertFalse(listener.isInvoked());
    }

    private void assertJoinHistoricalOnlyDependent(EPServiceProvider epService, String expression) {
        EPStatement stmt = epService.getEPAdministrator().createEPL(expression);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        String[] fields = "value,result".split(",");
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendSupportBeanEvent(epService, 5, 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{5, "|5|"}});

        sendSupportBeanEvent(epService, 1, 2);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{1, "|1|"}, {2, "|2|"}});

        sendSupportBeanEvent(epService, 0, -1);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, null);

        sendSupportBeanEvent(epService, 4, 6);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), fields, new Object[][]{{4, "|4|"}, {5, "|5|"}, {6, "|6|"}});

        stmt.destroy();
        sendSupportBeanEvent(epService, 0, -1);
        assertFalse(listener.isInvoked());
    }

    private void runAssertionNoJoinIterateVariables(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().createEPL("create variable int lower");
        epService.getEPAdministrator().createEPL("create variable int upper");
        epService.getEPAdministrator().createEPL("on SupportBean set lower=intPrimitive,upper=intBoxed");

        // Test int and singlerow
        String className = SupportStaticMethodLib.class.getName();
        String stmtText = "select value from method:" + className + ".fetchBetween(lower, upper)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"value"}, null);

        sendSupportBeanEvent(epService, 5, 10);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"value"}, new Object[][]{{5}, {6}, {7}, {8}, {9}, {10}});

        sendSupportBeanEvent(epService, 10, 5);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"value"}, null);

        sendSupportBeanEvent(epService, 4, 4);
        EPAssertionUtil.assertPropsPerRowAnyOrder(stmt.iterator(), new String[]{"value"}, new Object[][]{{4}});

        assertFalse(listener.isInvoked());
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryAssertionReturnTypeMultipleRow(EPServiceProvider epService, String method) {
        String epl = "select theString, intPrimitive, mapstring, mapint from " +
                SupportBean.class.getName() + "#keepall as s1, " +
                "method:" + SupportStaticMethodLib.class.getName() + "." + method;
        String[] fields = "theString,intPrimitive,mapstring,mapint".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendBeanEvent(epService, "E1", 0);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendBeanEvent(epService, "E2", -1);
        assertFalse(listener.isInvoked());
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, null);

        sendBeanEvent(epService, "E3", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 1, "|E3_0|", 100});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3", 1, "|E3_0|", 100}});

        sendBeanEvent(epService, "E4", 2);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"E4", 2, "|E4_0|", 100}, {"E4", 2, "|E4_1|", 101}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3", 1, "|E3_0|", 100}, {"E4", 2, "|E4_0|", 100}, {"E4", 2, "|E4_1|", 101}});

        sendBeanEvent(epService, "E5", 3);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields,
                new Object[][]{{"E5", 3, "|E5_0|", 100}, {"E5", 3, "|E5_1|", 101}, {"E5", 3, "|E5_2|", 102}});
        EPAssertionUtil.assertPropsPerRow(stmt.iterator(), fields, new Object[][]{{"E3", 1, "|E3_0|", 100},
            {"E4", 2, "|E4_0|", 100}, {"E4", 2, "|E4_1|", 101},
            {"E5", 3, "|E5_0|", 100}, {"E5", 3, "|E5_1|", 101}, {"E5", 3, "|E5_2|", 102}});

        listener.reset();
        stmt.destroy();
    }

    private void runAssertionDifferentReturnTypes(EPServiceProvider epService) {
        tryAssertionSingleRowFetch(epService, "fetchMap(theString, intPrimitive)");
        tryAssertionSingleRowFetch(epService, "fetchMapEventBean(s1, 'theString', 'intPrimitive')");
        tryAssertionSingleRowFetch(epService, "fetchObjectArrayEventBean(theString, intPrimitive)");
        tryAssertionSingleRowFetch(epService, "fetchPOJOArray(theString, intPrimitive)");
        tryAssertionSingleRowFetch(epService, "fetchPOJOCollection(theString, intPrimitive)");
        tryAssertionSingleRowFetch(epService, "fetchPOJOIterator(theString, intPrimitive)");

        tryAssertionReturnTypeMultipleRow(epService, "fetchMapArrayMR(theString, intPrimitive)");
        tryAssertionReturnTypeMultipleRow(epService, "fetchOAArrayMR(theString, intPrimitive)");
        tryAssertionReturnTypeMultipleRow(epService, "fetchPOJOArrayMR(theString, intPrimitive)");
        tryAssertionReturnTypeMultipleRow(epService, "fetchMapCollectionMR(theString, intPrimitive)");
        tryAssertionReturnTypeMultipleRow(epService, "fetchOACollectionMR(theString, intPrimitive)");
        tryAssertionReturnTypeMultipleRow(epService, "fetchPOJOCollectionMR(theString, intPrimitive)");
        tryAssertionReturnTypeMultipleRow(epService, "fetchMapIteratorMR(theString, intPrimitive)");
        tryAssertionReturnTypeMultipleRow(epService, "fetchOAIteratorMR(theString, intPrimitive)");
        tryAssertionReturnTypeMultipleRow(epService, "fetchPOJOIteratorMR(theString, intPrimitive)");
    }

    private void tryAssertionSingleRowFetch(EPServiceProvider epService, String method) {
        String epl = "select theString, intPrimitive, mapstring, mapint from " +
                SupportBean.class.getName() + " as s1, " +
                "method:" + SupportStaticMethodLib.class.getName() + "." + method;
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = new String[]{"theString", "intPrimitive", "mapstring", "mapint"};

        sendBeanEvent(epService, "E1", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E1", 1, "|E1|", 2});

        sendBeanEvent(epService, "E2", 3);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E2", 3, "|E2|", 4});

        sendBeanEvent(epService, "E3", 0);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"E3", 0, null, null});

        sendBeanEvent(epService, "E4", -1);
        assertFalse(listener.isInvoked());

        stmt.destroy();
    }

    private void runAssertionArrayNoArg(EPServiceProvider epService) {
        String joinStatement = "select id, theString from " +
                SupportBean.class.getName() + "#length(3) as s1, " +
                "method:" + SupportStaticMethodLib.class.getName() + ".fetchArrayNoArg";
        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        tryArrayNoArg(epService, stmt);

        joinStatement = "select id, theString from " +
                SupportBean.class.getName() + "#length(3) as s1, " +
                "method:" + SupportStaticMethodLib.class.getName() + ".fetchArrayNoArg()";
        stmt = epService.getEPAdministrator().createEPL(joinStatement);
        tryArrayNoArg(epService, stmt);

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(joinStatement);
        assertEquals(joinStatement, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        tryArrayNoArg(epService, stmt);

        model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("id", "theString"));
        model.setFromClause(FromClause.create()
                .add(FilterStream.create(SupportBean.class.getName(), "s1").addView("length", Expressions.constant(3)))
                .add(MethodInvocationStream.create(SupportStaticMethodLib.class.getName(), "fetchArrayNoArg")));
        stmt = epService.getEPAdministrator().create(model);
        assertEquals(joinStatement, model.toEPL());

        tryArrayNoArg(epService, stmt);
    }

    private void tryArrayNoArg(EPServiceProvider epService, EPStatement stmt) {
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = new String[]{"id", "theString"};

        sendBeanEvent(epService, "E1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"1", "E1"});

        sendBeanEvent(epService, "E2");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"1", "E2"});

        stmt.destroy();
    }

    private void runAssertionArrayWithArg(EPServiceProvider epService) {
        String joinStatement = "select irstream id, theString from " +
                SupportBean.class.getName() + "()#length(3) as s1, " +
                " method:" + SupportStaticMethodLib.class.getName() + ".fetchArrayGen(intPrimitive)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        tryArrayWithArg(epService, stmt);

        joinStatement = "select irstream id, theString from " +
                "method:" + SupportStaticMethodLib.class.getName() + ".fetchArrayGen(intPrimitive) as s0, " +
                SupportBean.class.getName() + "#length(3)";
        stmt = epService.getEPAdministrator().createEPL(joinStatement);
        tryArrayWithArg(epService, stmt);

        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(joinStatement);
        assertEquals(joinStatement, model.toEPL());
        stmt = epService.getEPAdministrator().create(model);
        tryArrayWithArg(epService, stmt);

        model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create("id", "theString").streamSelector(StreamSelector.RSTREAM_ISTREAM_BOTH));
        model.setFromClause(FromClause.create()
                .add(MethodInvocationStream.create(SupportStaticMethodLib.class.getName(), "fetchArrayGen", "s0")
                        .addParameter(Expressions.property("intPrimitive")))
                .add(FilterStream.create(SupportBean.class.getName()).addView("length", Expressions.constant(3)))
        );
        stmt = epService.getEPAdministrator().create(model);
        assertEquals(joinStatement, model.toEPL());

        tryArrayWithArg(epService, stmt);
    }

    private void tryArrayWithArg(EPServiceProvider epService, EPStatement stmt) {
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = new String[]{"id", "theString"};

        sendBeanEvent(epService, "E1", -1);
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, "E2", 0);
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, "E3", 1);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"A", "E3"});

        sendBeanEvent(epService, "E4", 2);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"A", "E4"}, {"B", "E4"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendBeanEvent(epService, "E5", 3);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"A", "E5"}, {"B", "E5"}, {"C", "E5"}});
        assertNull(listener.getLastOldData());
        listener.reset();

        sendBeanEvent(epService, "E6", 1);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"A", "E6"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][]{{"A", "E3"}});
        listener.reset();

        sendBeanEvent(epService, "E7", 1);
        EPAssertionUtil.assertPropsPerRow(listener.getLastNewData(), fields, new Object[][]{{"A", "E7"}});
        EPAssertionUtil.assertPropsPerRow(listener.getLastOldData(), fields, new Object[][]{{"A", "E4"}, {"B", "E4"}});
        listener.reset();

        stmt.destroy();
    }

    private void runAssertionObjectNoArg(EPServiceProvider epService) {
        String joinStatement = "select id, theString from " +
                SupportBean.class.getName() + "()#length(3) as s1, " +
                " method:" + SupportStaticMethodLib.class.getName() + ".fetchObjectNoArg()";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = new String[]{"id", "theString"};

        sendBeanEvent(epService, "E1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"2", "E1"});

        sendBeanEvent(epService, "E2");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"2", "E2"});
    }

    private void runAssertionObjectWithArg(EPServiceProvider epService) {
        String joinStatement = "select id, theString from " +
                SupportBean.class.getName() + "()#length(3) as s1, " +
                " method:" + SupportStaticMethodLib.class.getName() + ".fetchObject(theString)";

        EPStatement stmt = epService.getEPAdministrator().createEPL(joinStatement);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        String[] fields = new String[]{"id", "theString"};

        sendBeanEvent(epService, "E1");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"|E1|", "E1"});

        sendBeanEvent(epService, null);
        assertFalse(listener.isInvoked());

        sendBeanEvent(epService, "E2");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{"|E2|", "E2"});
    }

    private void runAssertionInvocationTargetEx(EPServiceProvider epService) {
        String joinStatement = "select s1.theString from " +
                SupportBean.class.getName() + "()#length(3) as s1, " +
                " method:" + SupportStaticMethodLib.class.getName() + ".throwExceptionBeanReturn()";

        epService.getEPAdministrator().createEPL(joinStatement);

        try {
            sendBeanEvent(epService, "E1");
            fail(); // default test configuration rethrows this exception
        } catch (EPException ex) {
            // fine
        }

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        tryInvalid(epService, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".fetchArrayGen()",
                "Error starting statement: Method footprint does not match the number or type of expression parameters, expecting no parameters in method: Could not find static method named 'fetchArrayGen' in class '" + SupportStaticMethodLib.class.getName() + "' taking no parameters (nearest match found was 'fetchArrayGen' taking type(s) 'int') [");

        tryInvalid(epService, "select * from SupportBean, method:.abc where 1=2",
                "Incorrect syntax near '.' at line 1 column 34, please check the method invocation join within the from clause [select * from SupportBean, method:.abc where 1=2]");

        tryInvalid(epService, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".fetchObjectAndSleep(1)",
                "Error starting statement: Method footprint does not match the number or type of expression parameters, expecting a method where parameters are typed 'int': Could not find static method named 'fetchObjectAndSleep' in class '" + SupportStaticMethodLib.class.getName() + "' with matching parameter number and expected parameter type(s) 'int' (nearest match found was 'fetchObjectAndSleep' taking type(s) 'String, int, long') [");

        tryInvalid(epService, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".sleep(100) where 1=2",
                "Error starting statement: Invalid return type for static method 'sleep' of class '" + SupportStaticMethodLib.class.getName() + "', expecting a Java class [select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".sleep(100) where 1=2]");

        tryInvalid(epService, "select * from SupportBean, method:AClass. where 1=2",
                "Incorrect syntax near 'where' (a reserved keyword) expecting an identifier but found 'where' at line 1 column 42, please check the view specifications within the from clause [select * from SupportBean, method:AClass. where 1=2]");

        tryInvalid(epService, "select * from SupportBean, method:Dummy.abc where 1=2",
                "Error starting statement: Could not load class by name 'Dummy', please check imports [select * from SupportBean, method:Dummy.abc where 1=2]");

        tryInvalid(epService, "select * from SupportBean, method:Math where 1=2",
                "Error starting statement: A function named 'Math' is not defined");

        tryInvalid(epService, "select * from SupportBean, method:Dummy.dummy()#length(100) where 1=2",
                "Error starting statement: Method data joins do not allow views onto the data, view 'length' is not valid in this context [select * from SupportBean, method:Dummy.dummy()#length(100) where 1=2]");

        tryInvalid(epService, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".dummy where 1=2",
                "Error starting statement: Could not find public static method named 'dummy' in class '" + SupportStaticMethodLib.class.getName() + "' [");

        tryInvalid(epService, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".minusOne(10) where 1=2",
                "Error starting statement: Invalid return type for static method 'minusOne' of class '" + SupportStaticMethodLib.class.getName() + "', expecting a Java class [");

        tryInvalid(epService, "select * from SupportBean, xyz:" + SupportStaticMethodLib.class.getName() + ".fetchArrayNoArg() where 1=2",
                "Expecting keyword 'method', found 'xyz' [select * from SupportBean, xyz:" + SupportStaticMethodLib.class.getName() + ".fetchArrayNoArg() where 1=2]");

        tryInvalid(epService, "select * from method:" + SupportStaticMethodLib.class.getName() + ".fetchBetween(s1.value, s1.value) as s0, method:" + SupportStaticMethodLib.class.getName() + ".fetchBetween(s0.value, s0.value) as s1",
                "Error starting statement: Circular dependency detected between historical streams [");

        tryInvalid(epService, "select * from method:" + SupportStaticMethodLib.class.getName() + ".fetchBetween(s0.value, s0.value) as s0, method:" + SupportStaticMethodLib.class.getName() + ".fetchBetween(s0.value, s0.value) as s1",
                "Error starting statement: Parameters for historical stream 0 indicate that the stream is subordinate to itself as stream parameters originate in the same stream [");

        tryInvalid(epService, "select * from method:" + SupportStaticMethodLib.class.getName() + ".fetchBetween(s0.value, s0.value) as s0",
                "Error starting statement: Parameters for historical stream 0 indicate that the stream is subordinate to itself as stream parameters originate in the same stream [");

        epService.getEPAdministrator().getConfiguration().addImport(SupportMethodInvocationJoinInvalid.class);
        tryInvalid(epService, "select * from method:SupportMethodInvocationJoinInvalid.readRowNoMetadata()",
                "Error starting statement: Could not find getter method for method invocation, expected a method by name 'readRowNoMetadataMetadata' accepting no parameters [select * from method:SupportMethodInvocationJoinInvalid.readRowNoMetadata()]");

        tryInvalid(epService, "select * from method:SupportMethodInvocationJoinInvalid.readRowWrongMetadata()",
                "Error starting statement: Getter method 'readRowWrongMetadataMetadata' does not return java.util.Map [select * from method:SupportMethodInvocationJoinInvalid.readRowWrongMetadata()]");

        tryInvalid(epService, "select * from SupportBean, method:" + SupportStaticMethodLib.class.getName() + ".invalidOverloadForJoin(null)",
                "Error starting statement: Method by name 'invalidOverloadForJoin' is overloaded in class '" + SupportStaticMethodLib.class.getName() + "' and overloaded methods do not return the same type");
    }

    private void tryAssertionUDFAndScriptReturningEvents(EPServiceProvider epService, String methodName) {
        EPStatement stmtSelect = epService.getEPAdministrator().createEPL("select id from SupportBean, method:" + methodName);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtSelect.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean());
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "id".split(","), new Object[][]{{"id1"}, {"id3"}});

        stmtSelect.destroy();
    }

    private void tryAssertionEventBeanArray(EPServiceProvider epService, String methodName, boolean soda) {
        String epl = "select p0 from SupportBean, method:" + SupportStaticMethodLib.class.getName() + "." + methodName + "(theString) @type(MyItemEvent)";
        EPStatement stmt = SupportModelHelper.createByCompileOrParse(epService, soda, epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("a,b", 0));
        EPAssertionUtil.assertPropsPerRow(listener.getAndResetLastNewData(), "p0".split(","), new Object[][]{{"a"}, {"b"}});

        stmt.destroy();
    }

    private void sendBeanEvent(EPServiceProvider epService, String theString) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendBeanEvent(EPServiceProvider epService, String theString, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(theString);
        bean.setIntPrimitive(intPrimitive);
        epService.getEPRuntime().sendEvent(bean);
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, int intPrimitive, int intBoxed) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setIntBoxed(intBoxed);
        epService.getEPRuntime().sendEvent(bean);
    }

    public static EventBean[] myItemProducerUDF(EPLMethodInvocationContext context) {
        EventBean[] events = new EventBean[2];
        int count = 0;
        for (String id : "id1,id3".split(",")) {
            events[count++] = context.getEventBeanService().adapterForMap(Collections.singletonMap("id", id), "ItemEvent");
        }
        return events;
    }

    public static SupportBean_S0 getStreamNameWContext(String a, SupportBean bean, EPLMethodInvocationContext context) {
        return new SupportBean_S0(1, a, bean.getTheString(), context.getStatementName());
    }

    public static SupportBean_S0 getWithMethodResultParam(String a, SupportBean bean, String b) {
        return new SupportBean_S0(1, a, bean.getTheString(), b);
    }

    public static String getWithMethodResultParamCompute(boolean param, EPLMethodInvocationContext context) {
        return context.getStatementName();
    }
}
