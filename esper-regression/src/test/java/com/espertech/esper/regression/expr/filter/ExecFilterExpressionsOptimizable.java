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
package com.espertech.esper.regression.expr.filter;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationPlugInSingleRowFunction;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.hook.EPLMethodInvocationContext;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.filterspec.FilterOperator;
import com.espertech.esper.filter.FilterServiceSPI;
import com.espertech.esper.filter.FilterSpecCompiler;
import com.espertech.esper.filterspec.FilterValueSetParam;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportFilterHelper;
import com.espertech.esper.supportregression.util.SupportFilterItem;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.SerializableObjectCopier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.espertech.esper.supportregression.util.SupportFilterItem.getBoolExprFilterItem;
import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecFilterExpressionsOptimizable implements RegressionExecution {
    private static final Logger log = LoggerFactory.getLogger(ExecFilterExpressionsOptimizable.class);
    private static EPLMethodInvocationContext methodInvocationContextFilterOptimized;

    public void configure(Configuration configuration) throws Exception {
        ConfigurationPlugInSingleRowFunction func = new ConfigurationPlugInSingleRowFunction();
        func.setFunctionClassName(this.getClass().getName());
        func.setFunctionMethodName("myCustomOkFunction");
        func.setFilterOptimizable(ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED);
        func.setRethrowExceptions(true);
        func.setName("myCustomOkFunction");
        configuration.getPlugInSingleRowFunctions().add(func);

        configuration.addEventType("SupportEvent", SupportTradeEvent.class);
        configuration.addEventType(SupportBean.class);
        configuration.addEventType(SupportBean_IntAlphabetic.class);
        configuration.addEventType(SupportBean_StringAlphabetic.class);
        configuration.getEngineDefaults().getExecution().setAllowIsolatedService(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("libE1True", MyLib.class.getName(), "libE1True", ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED);

        runAssertionInAndNotInKeywordMultivalue(epService);
        runAssertionOptimizablePerf(epService);
        runAssertionOptimizableInspectFilter(epService);
        runAssertionPatternUDFFilterOptimizable(epService);
        runAssertionOrToInRewrite(epService);
        runAssertionOrRewrite(epService);
        runAssertionOrPerformance(epService);
    }

    private void runAssertionInAndNotInKeywordMultivalue(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(MyEventInKeywordValue.class);

        tryInKeyword(epService, "ints", new MyEventInKeywordValue(new int[]{1, 2}));
        tryInKeyword(epService, "mapOfIntKey", new MyEventInKeywordValue(CollectionUtil.twoEntryMap(1, "x", 2, "y")));
        tryInKeyword(epService, "collOfInt", new MyEventInKeywordValue(Arrays.asList(1, 2)));

        tryNotInKeyword(epService, "ints", new MyEventInKeywordValue(new int[]{1, 2}));
        tryNotInKeyword(epService, "mapOfIntKey", new MyEventInKeywordValue(CollectionUtil.twoEntryMap(1, "x", 2, "y")));
        tryNotInKeyword(epService, "collOfInt", new MyEventInKeywordValue(Arrays.asList(1, 2)));

        tryInArrayContextProvided(epService);

        SupportMessageAssertUtil.tryInvalid(epService, "select * from pattern[every a=MyEventInKeywordValue -> SupportBean(intPrimitive in (a.longs))]",
                "Implicit conversion from datatype 'long' to 'Integer' for property 'intPrimitive' is not allowed (strict filter type coercion)");
    }

    private void runAssertionOptimizablePerf(EPServiceProvider epService) throws Exception {

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("libSplit", MyLib.class.getName(), "libSplit", ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED);

        // create listeners
        int count = 10;
        SupportUpdateListener[] listeners = new SupportUpdateListener[count];
        for (int i = 0; i < count; i++) {
            listeners[i] = new SupportUpdateListener();
        }

        // func(...) = value
        tryOptimizableEquals(epService, "select * from SupportBean(libSplit(theString) = !NUM!)", listeners);

        // func(...) implied true
        tryOptimizableBoolean(epService, "select * from SupportBean(libE1True(theString))");

        // declared expression (...) = value
        epService.getEPAdministrator().createEPL("create expression thesplit {theString => libSplit(theString)}");
        tryOptimizableEquals(epService, "select * from SupportBean(thesplit(*) = !NUM!)", listeners);

        // declared expression (...) implied true
        epService.getEPAdministrator().createEPL("create expression theE1Test {theString => libE1True(theString)}");
        tryOptimizableBoolean(epService, "select * from SupportBean(theE1Test(*))");

        // typeof(e)
        tryOptimizableTypeOf(epService);

        // with context
        tryOptimizableMethodInvocationContext(epService);

        // with variable and separate thread
        tryOptimizableVariableAndSeparateThread(epService);
    }

    private void tryOptimizableVariableAndSeparateThread(EPServiceProvider epService) throws Exception {

        epService.getEPAdministrator().getConfiguration().addVariable("myCheckServiceProvider", MyCheckServiceProvider.class, null);
        epService.getEPRuntime().setVariableValue("myCheckServiceProvider", new MyCheckServiceProvider());

        EPStatement epStatement = epService.getEPAdministrator().createEPL("select * from SupportBean(myCheckServiceProvider.check())");
        SupportUpdateListener listener = new SupportUpdateListener();
        epStatement.addListener(listener);
        CountDownLatch latch = new CountDownLatch(1);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            public void run() {
                epService.getEPRuntime().sendEvent(new SupportBean());
                assertTrue(listener.getIsInvokedAndReset());
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    private void runAssertionOptimizableInspectFilter(EPServiceProvider epService) {

        String epl;

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("funcOne", MyLib.class.getName(), "libSplit", ConfigurationPlugInSingleRowFunction.FilterOptimizable.DISABLED);
        epl = "select * from SupportBean(funcOne(theString) = 0)";
        assertFilterSingle(epService, epl, FilterSpecCompiler.PROPERTY_NAME_BOOLEAN_EXPRESSION, FilterOperator.BOOLEAN_EXPRESSION);

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("funcOneWDefault", MyLib.class.getName(), "libSplit");
        epl = "select * from SupportBean(funcOneWDefault(theString) = 0)";
        assertFilterSingle(epService, epl, "funcOneWDefault(theString)", FilterOperator.EQUAL);

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("funcTwo", MyLib.class.getName(), "libSplit", ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED);
        epl = "select * from SupportBean(funcTwo(theString) = 0)";
        assertFilterSingle(epService, epl, "funcTwo(theString)", FilterOperator.EQUAL);

        epl = "select * from SupportBean(libE1True(theString))";
        assertFilterSingle(epService, epl, "libE1True(theString)", FilterOperator.EQUAL);

        epl = "select * from SupportBean(funcTwo( theString ) > 10)";
        assertFilterSingle(epService, epl, "funcTwo(theString)", FilterOperator.GREATER);

        epService.getEPAdministrator().createEPL("create expression thesplit {theString => funcOne(theString)}");

        epl = "select * from SupportBean(thesplit(*) = 0)";
        assertFilterSingle(epService, epl, "thesplit(*)", FilterOperator.EQUAL);

        epl = "select * from SupportBean(libE1True(theString))";
        assertFilterSingle(epService, epl, "libE1True(theString)", FilterOperator.EQUAL);

        epl = "select * from SupportBean(thesplit(*) > 10)";
        assertFilterSingle(epService, epl, "thesplit(*)", FilterOperator.GREATER);

        epl = "expression housenumber alias for {10} select * from SupportBean(intPrimitive = housenumber)";
        assertFilterSingle(epService, epl, "intPrimitive", FilterOperator.EQUAL);

        epl = "expression housenumber alias for {intPrimitive*10} select * from SupportBean(intPrimitive = housenumber)";
        assertFilterSingle(epService, epl, ".boolean_expression", FilterOperator.BOOLEAN_EXPRESSION);

        epl = "select * from SupportBean(typeof(e) = 'SupportBean') as e";
        assertFilterSingle(epService, epl, "typeof(e)", FilterOperator.EQUAL);
    }

    private void runAssertionPatternUDFFilterOptimizable(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("myCustomBigDecimalEquals",
                this.getClass().getName(), "myCustomBigDecimalEquals");

        String epl = "select * from pattern[a=SupportBean() -> b=SupportBean(myCustomBigDecimalEquals(a.bigDecimal, b.bigDecimal))]";
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        SupportBean beanOne = new SupportBean("E1", 0);
        beanOne.setBigDecimal(BigDecimal.valueOf(13));
        epService.getEPRuntime().sendEvent(beanOne);

        SupportBean beanTwo = new SupportBean("E2", 0);
        beanTwo.setBigDecimal(BigDecimal.valueOf(13));
        epService.getEPRuntime().sendEvent(beanTwo);

        assertTrue(listener.isInvoked());
    }

    private void runAssertionOrToInRewrite(EPServiceProvider epService) {
        // test 'or' rewrite
        String[] filtersAB = new String[]{
            "theString = 'a' or theString = 'b'",
            "theString = 'a' or 'b' = theString",
            "'a' = theString or 'b' = theString",
            "'a' = theString or theString = 'b'",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            assertFilterSingle(epService, epl, "theString", FilterOperator.IN_LIST_OF_VALUES);
            SupportUpdateListener listener = new SupportUpdateListener();
            epService.getEPAdministrator().createEPL(epl).addListener(listener);

            epService.getEPRuntime().sendEvent(new SupportBean("a", 0));
            assertTrue(listener.getAndClearIsInvoked());
            epService.getEPRuntime().sendEvent(new SupportBean("b", 0));
            assertTrue(listener.getAndClearIsInvoked());
            epService.getEPRuntime().sendEvent(new SupportBean("c", 0));
            assertFalse(listener.getAndClearIsInvoked());

            epService.getEPAdministrator().destroyAllStatements();
        }

        String epl = "select * from SupportBean(intPrimitive = 1 and (theString='a' or theString='b'))";
        SupportFilterHelper.assertFilterTwo(epService, epl, "intPrimitive", FilterOperator.EQUAL, "theString", FilterOperator.IN_LIST_OF_VALUES);
    }

    private void runAssertionOrRewrite(EPServiceProvider epService) {
        tryOrRewriteTwoOr(epService);

        tryOrRewriteOrRewriteThreeOr(epService);

        tryOrRewriteOrRewriteWithAnd(epService);

        tryOrRewriteOrRewriteThreeWithOverlap(epService);

        tryOrRewriteOrRewriteFourOr(epService);

        tryOrRewriteOrRewriteEightOr(epService);

        tryOrRewriteAndRewriteNotEquals(epService);

        tryOrRewriteAndRewriteInnerOr(epService);

        tryOrRewriteOrRewriteAndOrMulti(epService);

        tryOrRewriteBooleanExprSimple(epService);

        tryOrRewriteBooleanExprAnd(epService);

        tryOrRewriteSubquery(epService);

        tryOrRewriteHint(epService);

        tryOrRewriteContextPartitionedSegmented(epService);

        tryOrRewriteContextPartitionedHash(epService);

        tryOrRewriteContextPartitionedCategory(epService);

        tryOrRewriteContextPartitionedInitiatedSameEvent(epService);

        tryOrRewriteContextPartitionedInitiated(epService);
    }

    private void runAssertionOrPerformance(EPServiceProvider epService) {
        for (Class clazz : new Class[]{SupportBean.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }
        SupportUpdateListener listener = new SupportUpdateListener();
        for (int i = 0; i < 1000; i++) {
            String epl = "select * from SupportBean(theString = '" + i + "' or intPrimitive=" + i + ")";
            epService.getEPAdministrator().createEPL(epl).addListener(listener);
        }

        long start = System.nanoTime();
        // System.out.println("Starting " + DateTime.print(new Date()));
        for (int i = 0; i < 10000; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("100", 1));
            assertTrue(listener.isInvoked());
            listener.reset();
        }
        // System.out.println("Ending " + DateTime.print(new Date()));
        double delta = (System.nanoTime() - start) / 1000d / 1000d;
        // System.out.println("Delta=" + (delta + " msec"));
        assertTrue(delta < 500);
    }

    private void tryInKeyword(EPServiceProvider epService, String field, MyEventInKeywordValue prototype) throws Exception {
        tryInKeywordPlain(epService, field, prototype);
        tryInKeywordPattern(epService, field, prototype);
    }

    private void tryNotInKeyword(EPServiceProvider epService, String field, MyEventInKeywordValue prototype) throws Exception {
        tryNotInKeywordPlain(epService, field, prototype);
        tryNotInKeywordPattern(epService, field, prototype);
    }

    private void tryInKeywordPlain(EPServiceProvider epService, String field, MyEventInKeywordValue prototype) throws Exception {

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyEventInKeywordValue#keepall where 1 in (" + field + ")");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(SerializableObjectCopier.copy(prototype));
        assertTrue(listener.getIsInvokedAndReset());

        stmt.destroy();
    }

    private void tryNotInKeywordPlain(EPServiceProvider epService, String field, MyEventInKeywordValue prototype) throws Exception {

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyEventInKeywordValue#keepall where 1 not in (" + field + ")");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(SerializableObjectCopier.copy(prototype));
        assertFalse(listener.getIsInvokedAndReset());

        stmt.destroy();
    }

    private void tryInKeywordPattern(EPServiceProvider epService, String field, MyEventInKeywordValue prototype) throws Exception {

        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from pattern[every a=MyEventInKeywordValue -> SupportBean(intPrimitive in (a." + field + "))]");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertInKeywordReceivedPattern(epService, listener, SerializableObjectCopier.copy(prototype), 1, true);
        assertInKeywordReceivedPattern(epService, listener, SerializableObjectCopier.copy(prototype), 2, true);

        assertInKeywordReceivedPattern(epService, listener, SerializableObjectCopier.copy(prototype), 3, false);
        SupportFilterHelper.assertFilterMulti(stmt, "SupportBean", new SupportFilterItem[][]{
                {new SupportFilterItem("intPrimitive", FilterOperator.IN_LIST_OF_VALUES)},
        });

        stmt.destroy();
    }

    private void tryNotInKeywordPattern(EPServiceProvider epService, String field, MyEventInKeywordValue prototype) throws Exception {

        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from pattern[every a=MyEventInKeywordValue -> SupportBean(intPrimitive not in (a." + field + "))]");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        assertInKeywordReceivedPattern(epService, listener, SerializableObjectCopier.copy(prototype), 0, true);
        assertInKeywordReceivedPattern(epService, listener, SerializableObjectCopier.copy(prototype), 3, true);

        assertInKeywordReceivedPattern(epService, listener, SerializableObjectCopier.copy(prototype), 1, false);
        SupportFilterHelper.assertFilterMulti(stmt, "SupportBean", new SupportFilterItem[][]{
                {new SupportFilterItem("intPrimitive", FilterOperator.NOT_IN_LIST_OF_VALUES)},
        });

        stmt.destroy();
    }

    private void assertInKeywordReceivedPattern(EPServiceProvider epService, SupportUpdateListener listener, Object event, int intPrimitive, boolean expected) throws Exception {
        epService.getEPRuntime().sendEvent(event);
        epService.getEPRuntime().sendEvent(new SupportBean(null, intPrimitive));
        assertEquals(expected, listener.getIsInvokedAndReset());
    }

    private void tryInArrayContextProvided(EPServiceProvider epService) {
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("create context MyContext initiated by MyEventInKeywordValue as mie terminated after 24 hours");

        EPStatement statementOne = epService.getEPAdministrator().createEPL("context MyContext select * from SupportBean#keepall where intPrimitive in (context.mie.ints)");
        statementOne.addListener(listenerOne);

        EPStatementSPI statementTwo = (EPStatementSPI) epService.getEPAdministrator().createEPL("context MyContext select * from SupportBean(intPrimitive in (context.mie.ints))");
        statementTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(new MyEventInKeywordValue(new int[]{1, 2}));

        assertInKeywordReceivedContext(epService, listenerOne, listenerTwo);

        SupportFilterHelper.assertFilterMulti(statementTwo, "SupportBean", new SupportFilterItem[][]{
                {new SupportFilterItem("intPrimitive", FilterOperator.IN_LIST_OF_VALUES)},
        });

        statementOne.destroy();
        statementTwo.destroy();
    }

    private void assertInKeywordReceivedContext(EPServiceProvider epService, SupportUpdateListener listenerOne, SupportUpdateListener listenerTwo) {
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertTrue(listenerOne.getIsInvokedAndReset() && listenerTwo.getIsInvokedAndReset());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertTrue(listenerOne.getIsInvokedAndReset() && listenerTwo.getIsInvokedAndReset());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listenerOne.getIsInvokedAndReset() || listenerTwo.getIsInvokedAndReset());
    }

    private void tryOrRewriteHint(EPServiceProvider epService) {
        String epl = "@Hint('MAX_FILTER_WIDTH=0') select * from SupportBean_IntAlphabetic((b=1 or c=1) and (d=1 or e=1))";
        assertFilterSingle(epService, epl, ".boolean_expression", FilterOperator.BOOLEAN_EXPRESSION);
    }

    private void tryOrRewriteSubquery(EPServiceProvider epService) {
        String epl = "select (select * from SupportBean_IntAlphabetic(a=1 or b=1)#keepall) as c0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean_IntAlphabetic iaOne = intEvent(1, 1);
        epService.getEPRuntime().sendEvent(iaOne);
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(iaOne, listener.assertOneGetNewAndReset().get("c0"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryOrRewriteContextPartitionedCategory(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context MyContext \n" +
                "  group a=1 or b=1 as g1,\n" +
                "  group c=1 as g1\n" +
                "  from SupportBean_IntAlphabetic");
        String epl = "context MyContext select * from SupportBean_IntAlphabetic(d=1 or e=1)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendAssertEvents(epService, listener,
                new Object[]{intEvent(1, 0, 0, 0, 1), intEvent(0, 1, 0, 1, 0), intEvent(0, 0, 1, 1, 1)},
                new Object[]{intEvent(0, 0, 0, 1, 0), intEvent(1, 0, 0, 0, 0), intEvent(0, 0, 1, 0, 0)}
        );
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryOrRewriteContextPartitionedHash(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context MyContext " +
                "coalesce by consistent_hash_crc32(a) from SupportBean_IntAlphabetic(b=1) granularity 16 preallocate");
        String epl = "context MyContext select * from SupportBean_IntAlphabetic(c=1 or d=1)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendAssertEvents(epService, listener,
                new Object[]{intEvent(100, 1, 0, 1), intEvent(100, 1, 1, 0)},
                new Object[]{intEvent(100, 0, 0, 1), intEvent(100, 1, 0, 0)}
        );
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryOrRewriteContextPartitionedSegmented(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context MyContext partition by a from SupportBean_IntAlphabetic(b=1 or c=1)");
        String epl = "context MyContext select * from SupportBean_IntAlphabetic(d=1)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        sendAssertEvents(epService, listener,
                new Object[]{intEvent(100, 1, 0, 1), intEvent(100, 0, 1, 1)},
                new Object[]{intEvent(100, 0, 0, 1), intEvent(100, 1, 0, 0)}
        );
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryOrRewriteBooleanExprAnd(EPServiceProvider epService) {
        String[] filters = new String[]{
            "(a='a' or a like 'A%') and (b='b' or b like 'B%')",
        };
        for (String filter : filters) {
            String epl = "select * from SupportBean_StringAlphabetic(" + filter + ")";
            EPStatement stmt = SupportFilterHelper.assertFilterMulti(epService, epl, "SupportBean_StringAlphabetic", new SupportFilterItem[][]{
                    {new SupportFilterItem("a", FilterOperator.EQUAL), new SupportFilterItem("b", FilterOperator.EQUAL)},
                    {new SupportFilterItem("a", FilterOperator.EQUAL), getBoolExprFilterItem()},
                    {new SupportFilterItem("b", FilterOperator.EQUAL), getBoolExprFilterItem()},
                    {getBoolExprFilterItem()},
            });
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            sendAssertEvents(epService, listener,
                    new Object[]{stringEvent("a", "b"), stringEvent("A1", "b"), stringEvent("a", "B1"), stringEvent("A1", "B1")},
                    new Object[]{stringEvent("x", "b"), stringEvent("a", "x"), stringEvent("A1", "C"), stringEvent("C", "B1")}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void tryOrRewriteBooleanExprSimple(EPServiceProvider epService) {
        String[] filters = new String[]{
            "a like 'a%' and (b='b' or c='c')",
        };
        for (String filter : filters) {
            String epl = "select * from SupportBean_StringAlphabetic(" + filter + ")";
            EPStatement stmt = SupportFilterHelper.assertFilterMulti(epService, epl, "SupportBean_StringAlphabetic", new SupportFilterItem[][]{
                    {new SupportFilterItem("b", FilterOperator.EQUAL), getBoolExprFilterItem()},
                    {new SupportFilterItem("c", FilterOperator.EQUAL), getBoolExprFilterItem()},
            });
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            sendAssertEvents(epService, listener,
                    new Object[]{stringEvent("a1", "b", null), stringEvent("a1", null, "c")},
                    new Object[]{stringEvent("x", "b", null), stringEvent("a1", null, null), stringEvent("a1", null, "x")}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void tryOrRewriteAndRewriteNotEquals(EPServiceProvider epService) {
        tryOrRewriteAndRewriteNotEqualsOr(epService);

        tryOrRewriteAndRewriteNotEqualsConsolidate(epService);

        tryOrRewriteAndRewriteNotEqualsWithOrConsolidateSecond(epService);
    }

    private void tryOrRewriteAndRewriteNotEqualsWithOrConsolidateSecond(EPServiceProvider epService) {
        String[] filters = new String[]{
            "a!=1 and a!=2 and ((a!=3 and a!=4) or (a!=5 and a!=6))",
        };
        for (String filter : filters) {
            String epl = "select * from SupportBean_IntAlphabetic(" + filter + ")";
            EPStatement stmt = SupportFilterHelper.assertFilterMulti(epService, epl, "SupportBean_IntAlphabetic", new SupportFilterItem[][]{
                    {new SupportFilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), getBoolExprFilterItem()},
                    {new SupportFilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), getBoolExprFilterItem()},
            });
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            sendAssertEvents(epService, listener,
                    new Object[]{intEvent(3), intEvent(4), intEvent(0)},
                    new Object[]{intEvent(2), intEvent(1)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void tryOrRewriteAndRewriteNotEqualsConsolidate(EPServiceProvider epService) {
        String[] filters = new String[]{
            "a!=1 and a!=2 and (a!=3 or a!=4)",
        };
        for (String filter : filters) {
            String epl = "select * from SupportBean_IntAlphabetic(" + filter + ")";
            EPStatement stmt = SupportFilterHelper.assertFilterMulti(epService, epl, "SupportBean_IntAlphabetic", new SupportFilterItem[][]{
                    {new SupportFilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new SupportFilterItem("a", FilterOperator.NOT_EQUAL)},
                    {new SupportFilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new SupportFilterItem("a", FilterOperator.NOT_EQUAL)},
            });
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            sendAssertEvents(epService, listener,
                    new Object[]{intEvent(3), intEvent(4), intEvent(0)},
                    new Object[]{intEvent(2), intEvent(1)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void tryOrRewriteAndRewriteNotEqualsOr(EPServiceProvider epService) {
        String[] filters = new String[]{
            "a!=1 and a!=2 and (b=1 or c=1)",
        };
        for (String filter : filters) {
            String epl = "select * from SupportBean_IntAlphabetic(" + filter + ")";
            EPStatement stmt = SupportFilterHelper.assertFilterMulti(epService, epl, "SupportBean_IntAlphabetic", new SupportFilterItem[][]{
                    {new SupportFilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new SupportFilterItem("b", FilterOperator.EQUAL)},
                    {new SupportFilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new SupportFilterItem("c", FilterOperator.EQUAL)},
            });
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            sendAssertEvents(epService, listener,
                    new Object[]{intEvent(3, 1, 0), intEvent(3, 0, 1), intEvent(0, 1, 0)},
                    new Object[]{intEvent(2, 0, 0), intEvent(1, 0, 0), intEvent(3, 0, 0)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void tryOrRewriteAndRewriteInnerOr(EPServiceProvider epService) {
        String[] filtersAB = new String[]{
            "theString='a' and (intPrimitive=1 or longPrimitive=10)",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = SupportFilterHelper.assertFilterMulti(epService, epl, "SupportBean", new SupportFilterItem[][]{
                    {new SupportFilterItem("theString", FilterOperator.EQUAL), new SupportFilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new SupportFilterItem("theString", FilterOperator.EQUAL), new SupportFilterItem("longPrimitive", FilterOperator.EQUAL)},
            });
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            sendAssertEvents(epService, listener,
                    new SupportBean[]{makeEvent("a", 1, 0), makeEvent("a", 0, 10), makeEvent("a", 1, 10)},
                    new SupportBean[]{makeEvent("x", 0, 0), makeEvent("a", 2, 20), makeEvent("x", 1, 10)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void tryOrRewriteOrRewriteAndOrMulti(EPServiceProvider epService) {
        String[] filtersAB = new String[]{
            "a=1 and (b=1 or c=1) and (d=1 or e=1)",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean_IntAlphabetic(" + filter + ")";
            EPStatement stmt = SupportFilterHelper.assertFilterMulti(epService, epl, "SupportBean_IntAlphabetic", new SupportFilterItem[][]{
                    {new SupportFilterItem("a", FilterOperator.EQUAL), new SupportFilterItem("b", FilterOperator.EQUAL), new SupportFilterItem("d", FilterOperator.EQUAL)},
                    {new SupportFilterItem("a", FilterOperator.EQUAL), new SupportFilterItem("c", FilterOperator.EQUAL), new SupportFilterItem("d", FilterOperator.EQUAL)},
                    {new SupportFilterItem("a", FilterOperator.EQUAL), new SupportFilterItem("c", FilterOperator.EQUAL), new SupportFilterItem("e", FilterOperator.EQUAL)},
                    {new SupportFilterItem("a", FilterOperator.EQUAL), new SupportFilterItem("b", FilterOperator.EQUAL), new SupportFilterItem("e", FilterOperator.EQUAL)},
            });
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            sendAssertEvents(epService, listener,
                    new Object[]{intEvent(1, 1, 0, 1, 0), intEvent(1, 0, 1, 0, 1), intEvent(1, 1, 0, 0, 1), intEvent(1, 0, 1, 1, 0)},
                    new Object[]{intEvent(1, 0, 0, 1, 0), intEvent(1, 0, 0, 1, 0), intEvent(1, 1, 1, 0, 0), intEvent(0, 1, 1, 1, 1)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void tryOrRewriteOrRewriteEightOr(EPServiceProvider epService) {
        String[] filtersAB = new String[]{
            "theString = 'a' or intPrimitive=1 or longPrimitive=10 or doublePrimitive=100 or boolPrimitive=true or " +
                    "intBoxed=2 or longBoxed=20 or doubleBoxed=200",
            "longBoxed=20 or theString = 'a' or boolPrimitive=true or intBoxed=2 or longPrimitive=10 or doublePrimitive=100 or " +
                    "intPrimitive=1 or doubleBoxed=200",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = SupportFilterHelper.assertFilterMulti(epService, epl, "SupportBean", new SupportFilterItem[][]{
                    {new SupportFilterItem("theString", FilterOperator.EQUAL)},
                    {new SupportFilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new SupportFilterItem("longPrimitive", FilterOperator.EQUAL)},
                    {new SupportFilterItem("doublePrimitive", FilterOperator.EQUAL)},
                    {new SupportFilterItem("boolPrimitive", FilterOperator.EQUAL)},
                    {new SupportFilterItem("intBoxed", FilterOperator.EQUAL)},
                    {new SupportFilterItem("longBoxed", FilterOperator.EQUAL)},
                    {new SupportFilterItem("doubleBoxed", FilterOperator.EQUAL)},
            });
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            sendAssertEvents(epService, listener,
                    new SupportBean[]{makeEvent("a", 1, 10, 100, true, 2, 20, 200), makeEvent("a", 0, 0, 0, true, 0, 0, 0),
                            makeEvent("a", 0, 0, 0, true, 0, 20, 0), makeEvent("x", 0, 0, 100, false, 0, 0, 0),
                            makeEvent("x", 1, 0, 0, false, 0, 0, 200), makeEvent("x", 0, 0, 0, false, 0, 0, 200),
                    },
                    new SupportBean[]{makeEvent("x", 0, 0, 0, false, 0, 0, 0)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void tryOrRewriteOrRewriteFourOr(EPServiceProvider epService) {
        String[] filtersAB = new String[]{
            "theString = 'a' or intPrimitive=1 or longPrimitive=10 or doublePrimitive=100",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = SupportFilterHelper.assertFilterMulti(epService, epl, "SupportBean", new SupportFilterItem[][]{
                    {new SupportFilterItem("theString", FilterOperator.EQUAL)},
                    {new SupportFilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new SupportFilterItem("longPrimitive", FilterOperator.EQUAL)},
                    {new SupportFilterItem("doublePrimitive", FilterOperator.EQUAL)},
            });
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            sendAssertEvents(epService, listener,
                    new SupportBean[]{makeEvent("a", 1, 10, 100), makeEvent("x", 0, 0, 100), makeEvent("x", 0, 10, 100), makeEvent("a", 0, 0, 0)},
                    new SupportBean[]{makeEvent("x", 0, 0, 0)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void tryOrRewriteOrRewriteThreeWithOverlap(EPServiceProvider epService) {
        String[] filtersAB = new String[]{
            "theString = 'a' or theString = 'b' or intPrimitive=1",
            "intPrimitive = 1 or theString = 'b' or theString = 'a'",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = SupportFilterHelper.assertFilterMulti(epService, epl, "SupportBean", new SupportFilterItem[][]{
                    {new SupportFilterItem("theString", FilterOperator.EQUAL)},
                    {new SupportFilterItem("theString", FilterOperator.EQUAL)},
                    {new SupportFilterItem("intPrimitive", FilterOperator.EQUAL)},
            });
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            sendAssertEvents(epService, listener,
                    new SupportBean[]{makeEvent("a", 1), makeEvent("b", 0), makeEvent("x", 1)},
                    new SupportBean[]{makeEvent("x", 0)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void tryOrRewriteOrRewriteWithAnd(EPServiceProvider epService) {
        String[] filtersAB = new String[]{
            "(theString = 'a' and intPrimitive = 1) or (theString = 'b' and intPrimitive = 2)",
            "(intPrimitive = 1 and theString = 'a') or (intPrimitive = 2 and theString = 'b')",
            "(theString = 'b' and intPrimitive = 2) or (theString = 'a' and intPrimitive = 1)",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = SupportFilterHelper.assertFilterMulti(epService, epl, "SupportBean", new SupportFilterItem[][]{
                    {new SupportFilterItem("theString", FilterOperator.EQUAL), new SupportFilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new SupportFilterItem("theString", FilterOperator.EQUAL), new SupportFilterItem("intPrimitive", FilterOperator.EQUAL)},
            });
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            sendAssertEvents(epService, listener,
                    new SupportBean[]{makeEvent("a", 1), makeEvent("b", 2)},
                    new SupportBean[]{makeEvent("x", 0), makeEvent("a", 0), makeEvent("a", 2), makeEvent("b", 1)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void tryOrRewriteOrRewriteThreeOr(EPServiceProvider epService) {
        String[] filtersAB = new String[]{
            "theString = 'a' or intPrimitive = 1 or longPrimitive = 2",
            "2 = longPrimitive or 1 = intPrimitive or theString = 'a'"
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = SupportFilterHelper.assertFilterMulti(epService, epl, "SupportBean", new SupportFilterItem[][]{
                    {new SupportFilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new SupportFilterItem("theString", FilterOperator.EQUAL)},
                    {new SupportFilterItem("longPrimitive", FilterOperator.EQUAL)},
            });
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            sendAssertEvents(epService, listener,
                    new SupportBean[]{makeEvent("a", 0, 0), makeEvent("b", 1, 0), makeEvent("c", 0, 2), makeEvent("c", 0, 2)},
                    new SupportBean[]{makeEvent("v", 0, 0), makeEvent("c", 2, 1)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void sendAssertEvents(EPServiceProvider epService, SupportUpdateListener listener, Object[] matches, Object[] nonMatches) {
        listener.reset();
        for (Object match : matches) {
            epService.getEPRuntime().sendEvent(match);
            assertSame(match, listener.assertOneGetNewAndReset().getUnderlying());
        }
        listener.reset();
        for (Object nonMatch : nonMatches) {
            epService.getEPRuntime().sendEvent(nonMatch);
            assertFalse(listener.isInvoked());
        }
    }

    private void tryOrRewriteTwoOr(EPServiceProvider epService) {
        // test 'or' rewrite
        String[] filtersAB = new String[]{
            "theString = 'a' or intPrimitive = 1",
            "theString = 'a' or 1 = intPrimitive",
            "'a' = theString or 1 = intPrimitive",
            "'a' = theString or intPrimitive = 1",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = SupportFilterHelper.assertFilterMulti(epService, epl, "SupportBean", new SupportFilterItem[][]{
                    {new SupportFilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new SupportFilterItem("theString", FilterOperator.EQUAL)},
            });
            SupportUpdateListener listener = new SupportUpdateListener();
            stmt.addListener(listener);

            epService.getEPRuntime().sendEvent(new SupportBean("a", 0));
            listener.assertOneGetNewAndReset();
            epService.getEPRuntime().sendEvent(new SupportBean("b", 1));
            listener.assertOneGetNewAndReset();
            epService.getEPRuntime().sendEvent(new SupportBean("c", 0));
            assertFalse(listener.getAndClearIsInvoked());

            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void tryOptimizableEquals(EPServiceProvider epService, String epl, SupportUpdateListener[] listeners) {

        // test function returns lookup value and "equals"
        for (int i = 0; i < listeners.length; i++) {
            EPStatement stmt = epService.getEPAdministrator().createEPL(epl.replace("!NUM!", Integer.toString(i)));
            stmt.addListener(listeners[i]);
        }

        long startTime = System.currentTimeMillis();
        MyLib.resetCountInvoked();
        int loops = 1000;
        for (int i = 0; i < loops; i++) {
            epService.getEPRuntime().sendEvent(new SupportBean("E_" + i % listeners.length, 0));
            SupportUpdateListener listener = listeners[i % listeners.length];
            assertTrue(listener.getAndClearIsInvoked());
        }
        long delta = System.currentTimeMillis() - startTime;
        assertEquals(loops, MyLib.getCountInvoked());

        log.info("Equals delta=" + delta);
        assertTrue("Delta is " + delta, delta < 1000);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryOptimizableBoolean(EPServiceProvider epService, String epl) {

        // test function returns lookup value and "equals"
        int count = 10;
        SupportUpdateListener listener = new SupportUpdateListener();
        for (int i = 0; i < count; i++) {
            EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
            stmt.addListener(listener);
        }

        long startTime = System.currentTimeMillis();
        MyLib.resetCountInvoked();
        int loops = 10000;
        for (int i = 0; i < loops; i++) {
            String key = "E_" + i % 100;
            epService.getEPRuntime().sendEvent(new SupportBean(key, 0));
            if (key.equals("E_1")) {
                assertEquals(count, listener.getNewDataList().size());
                listener.reset();
            } else {
                assertFalse(listener.isInvoked());
            }
        }
        long delta = System.currentTimeMillis() - startTime;
        assertEquals(loops, MyLib.getCountInvoked());

        log.info("Boolean delta=" + delta);
        assertTrue("Delta is " + delta, delta < 1000);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertFilterSingle(EPServiceProvider epService, String epl, String expression, FilterOperator op) {
        EPStatementSPI statementSPI = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        if (((FilterServiceSPI) statementSPI.getStatementContext().getFilterService()).isSupportsTakeApply()) {
            FilterValueSetParam param = SupportFilterHelper.getFilterSingle(statementSPI);
            assertEquals("failed for '" + epl + "'", op, param.getFilterOperator());
            assertEquals(expression, param.getLookupable().getExpression());
        }
    }

    private void tryOptimizableMethodInvocationContext(EPServiceProvider epService) {
        methodInvocationContextFilterOptimized = null;
        epService.getEPAdministrator().createEPL("select * from SupportBean e where myCustomOkFunction(e) = \"OK\"");
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals("default", methodInvocationContextFilterOptimized.getEngineURI());
        assertEquals("myCustomOkFunction", methodInvocationContextFilterOptimized.getFunctionName());
        assertNull(methodInvocationContextFilterOptimized.getStatementUserObject());
        assertNull(methodInvocationContextFilterOptimized.getStatementName());
        assertEquals(-1, methodInvocationContextFilterOptimized.getContextPartitionId());
        methodInvocationContextFilterOptimized = null;
    }

    private void tryOptimizableTypeOf(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportOverrideBase.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportOverrideBase(typeof(e) = 'SupportOverrideBase') as e");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportOverrideBase(""));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportOverrideOne("a", "b"));
        assertFalse(listener.getAndClearIsInvoked());

        stmt.destroy();
    }

    private void tryOrRewriteContextPartitionedInitiated(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("@name('ctx') create context MyContext initiated by SupportBean(theString='A' or intPrimitive=1) terminated after 24 hours");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("@name('select') context MyContext select * from SupportBean").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        listener.assertOneGetNewAndReset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void tryOrRewriteContextPartitionedInitiatedSameEvent(EPServiceProvider epService) {
        epService.getEPAdministrator().createEPL("create context MyContext initiated by SupportBean terminated after 24 hours");
        SupportUpdateListener listener = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("context MyContext select * from SupportBean(theString='A' or intPrimitive=1)").addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("A", 1));
        listener.assertOneGetNewAndReset();

        epService.getEPAdministrator().destroyAllStatements();
    }

    private SupportBean makeEvent(String theString, int intPrimitive) {
        return makeEvent(theString, intPrimitive, 0L);
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive) {
        return makeEvent(theString, intPrimitive, longPrimitive, 0d);
    }

    private SupportBean_IntAlphabetic intEvent(int a) {
        return new SupportBean_IntAlphabetic(a);
    }

    private SupportBean_IntAlphabetic intEvent(int a, int b) {
        return new SupportBean_IntAlphabetic(a, b);
    }

    private SupportBean_IntAlphabetic intEvent(int a, int b, int c, int d) {
        return new SupportBean_IntAlphabetic(a, b, c, d);
    }

    private SupportBean_StringAlphabetic stringEvent(String a, String b) {
        return new SupportBean_StringAlphabetic(a, b);
    }

    private SupportBean_StringAlphabetic stringEvent(String a, String b, String c) {
        return new SupportBean_StringAlphabetic(a, b, c);
    }

    private SupportBean_IntAlphabetic intEvent(int a, int b, int c) {
        return new SupportBean_IntAlphabetic(a, b, c);
    }

    private SupportBean_IntAlphabetic intEvent(int a, int b, int c, int d, int e) {
        return new SupportBean_IntAlphabetic(a, b, c, d, e);
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive, double doublePrimitive) {
        SupportBean event = new SupportBean(theString, intPrimitive);
        event.setLongPrimitive(longPrimitive);
        event.setDoublePrimitive(doublePrimitive);
        return event;
    }

    private SupportBean makeEvent(String theString, int intPrimitive, long longPrimitive, double doublePrimitive,
                                  boolean boolPrimitive, int intBoxed, long longBoxed, double doubleBoxed) {
        SupportBean event = new SupportBean(theString, intPrimitive);
        event.setLongPrimitive(longPrimitive);
        event.setDoublePrimitive(doublePrimitive);
        event.setBoolPrimitive(boolPrimitive);
        event.setLongBoxed(longBoxed);
        event.setDoubleBoxed(doubleBoxed);
        event.setIntBoxed(intBoxed);
        return event;
    }

    public static String myCustomOkFunction(Object e, EPLMethodInvocationContext ctx) {
        methodInvocationContextFilterOptimized = ctx;
        return "OK";
    }

    public static boolean myCustomBigDecimalEquals(final BigDecimal first, final BigDecimal second) {
        return first.compareTo(second) == 0;
    }

    public static class MyLib {

        private static int countInvoked;

        public static int libSplit(String theString) {
            String[] key = theString.split("_");
            countInvoked++;
            return Integer.parseInt(key[1]);
        }

        public static boolean libE1True(String theString) {
            countInvoked++;
            return theString.equals("E_1");
        }

        public static int getCountInvoked() {
            return countInvoked;
        }

        public static void resetCountInvoked() {
            countInvoked = 0;
        }
    }

    public static class MyEventInKeywordValue implements Serializable {
        private int[] ints;
        private long[] longs;
        private Map<Integer, String> mapOfIntKey;
        private Collection<Integer> collOfInt;

        public MyEventInKeywordValue(int[] ints) {
            this.ints = ints;
        }

        public MyEventInKeywordValue(Map<Integer, String> mapOfIntKey) {
            this.mapOfIntKey = mapOfIntKey;
        }

        public MyEventInKeywordValue(Collection<Integer> collOfInt) {
            this.collOfInt = collOfInt;
        }

        public MyEventInKeywordValue(long[] longs) {
            this.longs = longs;
        }

        public int[] getInts() {
            return ints;
        }

        public Map<Integer, String> getMapOfIntKey() {
            return mapOfIntKey;
        }

        public Collection<Integer> getCollOfInt() {
            return collOfInt;
        }

        public long[] getLongs() {
            return longs;
        }
    }

    public static class MyCheckServiceProvider {
        public boolean check() {
            return true;
        }
    }
}
