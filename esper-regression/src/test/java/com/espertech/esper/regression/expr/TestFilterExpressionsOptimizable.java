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

package com.espertech.esper.regression.expr;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.core.service.EPStatementSPI;
import com.espertech.esper.filter.*;
import com.espertech.esper.supportregression.bean.*;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

public class TestFilterExpressionsOptimizable extends TestCase
{
    private static final Logger log = LoggerFactory.getLogger(TestFilterExpressionsOptimizable.class);

    private EPServiceProvider epService;
    private SupportUpdateListener listener;

    public void setUp()
    {
        listener = new SupportUpdateListener();

        Configuration config = SupportConfigFactory.getConfiguration();
        config.addEventType("SupportEvent", SupportTradeEvent.class);
        config.addEventType(SupportBean.class);
        config.addEventType(SupportBean_IntAlphabetic.class);
        config.addEventType(SupportBean_StringAlphabetic.class);
        config.getEngineDefaults().getExecution().setAllowIsolatedService(true);

        epService = EPServiceProviderManager.getDefaultProvider(config);
        epService.initialize();
    }

    protected void tearDown() throws Exception {
        listener = null;
    }

    public void testInAndNotInKeywordMultivalue() throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType(MyEventInKeywordValue.class);

        runAssertionInKeyword("ints", new MyEventInKeywordValue(new int[] {1, 2}));
        runAssertionInKeyword("mapOfIntKey", new MyEventInKeywordValue(CollectionUtil.twoEntryMap(1, "x", 2, "y")));
        runAssertionInKeyword("collOfInt", new MyEventInKeywordValue(Arrays.asList(1, 2)));

        runAssertionNotInKeyword("ints", new MyEventInKeywordValue(new int[] {1, 2}));
        runAssertionNotInKeyword("mapOfIntKey", new MyEventInKeywordValue(CollectionUtil.twoEntryMap(1, "x", 2, "y")));
        runAssertionNotInKeyword("collOfInt", new MyEventInKeywordValue(Arrays.asList(1, 2)));

        runAssertionInArrayContextProvided();

        SupportMessageAssertUtil.tryInvalid(epService, "select * from pattern[every a=MyEventInKeywordValue -> SupportBean(intPrimitive in (a.longs))]",
           "Implicit conversion from datatype 'long' to 'int' for property 'intPrimitive' is not allowed (strict filter type coercion)");
    }

    public void testOptimizablePerf() {

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("libSplit", MyLib.class.getName(), "libSplit", ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED);
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("libE1True", MyLib.class.getName(), "libE1True", ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED);

        // create listeners
        int count = 10;
        SupportUpdateListener[] listeners = new SupportUpdateListener[count];
        for (int i = 0; i < count; i++) {
            listeners[i] = new SupportUpdateListener();
        }

        // func(...) = value
        runAssertionEquals("select * from SupportBean(libSplit(theString) = !NUM!)", listeners);

        // func(...) implied true
        runAssertionBoolean("select * from SupportBean(libE1True(theString))");

        // declared expression (...) = value
        epService.getEPAdministrator().createEPL("create expression thesplit {theString => libSplit(theString)}");
        runAssertionEquals("select * from SupportBean(thesplit(*) = !NUM!)", listeners);

        // declared expression (...) implied true
        epService.getEPAdministrator().createEPL("create expression theE1Test {theString => libE1True(theString)}");
        runAssertionBoolean("select * from SupportBean(theE1Test(*))");

        // typeof(e)
        runAssertionTypeOf();
    }

    public void testOptimizableInspectFilter() {

        String epl;

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("funcOne", MyLib.class.getName(), "libSplit", ConfigurationPlugInSingleRowFunction.FilterOptimizable.DISABLED);
        epl = "select * from SupportBean(funcOne(theString) = 0)";
        assertFilterSingle(epl, FilterSpecCompiler.PROPERTY_NAME_BOOLEAN_EXPRESSION, FilterOperator.BOOLEAN_EXPRESSION);

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("funcOneWDefault", MyLib.class.getName(), "libSplit");
        epl = "select * from SupportBean(funcOneWDefault(theString) = 0)";
        assertFilterSingle(epl, "funcOneWDefault(theString)", FilterOperator.EQUAL);

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("funcTwo", MyLib.class.getName(), "libSplit", ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED);
        epl = "select * from SupportBean(funcTwo(theString) = 0)";
        assertFilterSingle(epl, "funcTwo(theString)", FilterOperator.EQUAL);

        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("libE1True", MyLib.class.getName(), "libE1True", ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED);
        epl = "select * from SupportBean(libE1True(theString))";
        assertFilterSingle(epl, "libE1True(theString)", FilterOperator.EQUAL);

        epl = "select * from SupportBean(funcTwo( theString ) > 10)";
        assertFilterSingle(epl, "funcTwo(theString)", FilterOperator.GREATER);

        epService.getEPAdministrator().createEPL("create expression thesplit {theString => funcOne(theString)}");

        epl = "select * from SupportBean(thesplit(*) = 0)";
        assertFilterSingle(epl, "thesplit(*)", FilterOperator.EQUAL);

        epl = "select * from SupportBean(libE1True(theString))";
        assertFilterSingle(epl, "libE1True(theString)", FilterOperator.EQUAL);

        epl = "select * from SupportBean(thesplit(*) > 10)";
        assertFilterSingle(epl, "thesplit(*)", FilterOperator.GREATER);

        epl = "expression housenumber alias for {10} select * from SupportBean(intPrimitive = housenumber)";
        assertFilterSingle(epl, "intPrimitive", FilterOperator.EQUAL);

        epl = "expression housenumber alias for {intPrimitive*10} select * from SupportBean(intPrimitive = housenumber)";
        assertFilterSingle(epl, ".boolean_expression", FilterOperator.BOOLEAN_EXPRESSION);

        epl = "select * from SupportBean(typeof(e) = 'SupportBean') as e";
        assertFilterSingle(epl, "typeof(e)", FilterOperator.EQUAL);
    }

    public void testPatternUDFFilterOptimizable() {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("myCustomBigDecimalEquals",
                this.getClass().getName(), "myCustomBigDecimalEquals");

        String epl = "select * from pattern[a=SupportBean() -> b=SupportBean(myCustomBigDecimalEquals(a.bigDecimal, b.bigDecimal))]";
        epService.getEPAdministrator().createEPL(epl).addListener(listener);

        SupportBean beanOne = new SupportBean("E1", 0);
        beanOne.setBigDecimal(BigDecimal.valueOf(13));
        epService.getEPRuntime().sendEvent(beanOne);

        SupportBean beanTwo = new SupportBean("E2", 0);
        beanTwo.setBigDecimal(BigDecimal.valueOf(13));
        epService.getEPRuntime().sendEvent(beanTwo);

        assertTrue(listener.isInvoked());
    }

    public void testOrToInRewrite()
    {
        // test 'or' rewrite
        String[] filtersAB = new String[] {
                "theString = 'a' or theString = 'b'",
                "theString = 'a' or 'b' = theString",
                "'a' = theString or 'b' = theString",
                "'a' = theString or theString = 'b'",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            assertFilterSingle(epl, "theString", FilterOperator.IN_LIST_OF_VALUES);
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
        assertFilterTwo(epl, "intPrimitive", FilterOperator.EQUAL, "theString", FilterOperator.IN_LIST_OF_VALUES);
    }

    public void testOrRewrite()
    {
        runAssertionOrRewriteTwoOr();

        runAssertionOrRewriteThreeOr();

        runAssertionOrRewriteWithAnd();

        runAssertionOrRewriteThreeWithOverlap();

        runAssertionOrRewriteFourOr();

        runAssertionOrRewriteEightOr();

        runAssertionAndRewriteNotEquals();

        runAssertionAndRewriteInnerOr();

        runAssertionOrRewriteAndOrMulti();

        runAssertionBooleanExprSimple();

        runAssertionBooleanExprAnd();

        runAssertionContextPartitionedSegmented();

        runAssertionContextPartitionedHash();

        runAssertionContextPartitionedCategory();

        runAssertionSubquery();

        runAssertionHint();
    }

    public void testOrPerformance()
    {
        for (Class clazz : new Class[] {SupportBean.class}) {
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

    private void runAssertionInKeyword(String field, MyEventInKeywordValue prototype) throws Exception {
        runAssertionInKeywordPlain(field, prototype);
        runAssertionInKeywordPattern(field, prototype);
    }

    private void runAssertionNotInKeyword(String field, MyEventInKeywordValue prototype) throws Exception {
        runAssertionNotInKeywordPlain(field, prototype);
        runAssertionNotInKeywordPattern(field, prototype);
    }

    private void runAssertionInKeywordPlain(String field, MyEventInKeywordValue prototype) throws Exception {

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyEventInKeywordValue#keepall where 1 in (" + field + ")");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(SerializableObjectCopier.copy(prototype));
        assertTrue(listener.getIsInvokedAndReset());

        stmt.destroy();
    }

    private void runAssertionNotInKeywordPlain(String field, MyEventInKeywordValue prototype) throws Exception {

        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from MyEventInKeywordValue#keepall where 1 not in (" + field + ")");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(SerializableObjectCopier.copy(prototype));
        assertFalse(listener.getIsInvokedAndReset());

        stmt.destroy();
    }

    private void runAssertionInKeywordPattern(String field, MyEventInKeywordValue prototype) throws Exception {

        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from pattern[every a=MyEventInKeywordValue -> SupportBean(intPrimitive in (a." + field + "))]");
        stmt.addListener(listener);

        assertInKeywordReceivedPattern(SerializableObjectCopier.copy(prototype), 1, true);
        assertInKeywordReceivedPattern(SerializableObjectCopier.copy(prototype), 2, true);

        assertInKeywordReceivedPattern(SerializableObjectCopier.copy(prototype), 3, false);
        assertFilterMulti("SupportBean", stmt, new FilterItem[][] {
                {new FilterItem("intPrimitive", FilterOperator.IN_LIST_OF_VALUES)},
        });

        stmt.destroy();
    }

    private void runAssertionNotInKeywordPattern(String field, MyEventInKeywordValue prototype) throws Exception {

        EPStatementSPI stmt = (EPStatementSPI) epService.getEPAdministrator().createEPL("select * from pattern[every a=MyEventInKeywordValue -> SupportBean(intPrimitive not in (a." + field + "))]");
        stmt.addListener(listener);

        assertInKeywordReceivedPattern(SerializableObjectCopier.copy(prototype), 0, true);
        assertInKeywordReceivedPattern(SerializableObjectCopier.copy(prototype), 3, true);

        assertInKeywordReceivedPattern(SerializableObjectCopier.copy(prototype), 1, false);
        assertFilterMulti("SupportBean", stmt, new FilterItem[][] {
                {new FilterItem("intPrimitive", FilterOperator.NOT_IN_LIST_OF_VALUES)},
        });

        stmt.destroy();
    }

    private void assertInKeywordReceivedPattern(Object event, int intPrimitive, boolean expected) throws Exception {
        epService.getEPRuntime().sendEvent(event);
        epService.getEPRuntime().sendEvent(new SupportBean(null, intPrimitive));
        assertEquals(expected, listener.getIsInvokedAndReset());
    }

    private void runAssertionInArrayContextProvided() {
        SupportUpdateListener listenerOne = new SupportUpdateListener();
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        epService.getEPAdministrator().createEPL("create context MyContext initiated by MyEventInKeywordValue as mie terminated after 24 hours");

        EPStatement statementOne = epService.getEPAdministrator().createEPL("context MyContext select * from SupportBean#keepall where intPrimitive in (context.mie.ints)");
        statementOne.addListener(listenerOne);

        EPStatementSPI statementTwo = (EPStatementSPI) epService.getEPAdministrator().createEPL("context MyContext select * from SupportBean(intPrimitive in (context.mie.ints))");
        statementTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(new MyEventInKeywordValue(new int[] {1, 2}));

        assertInKeywordReceivedContext(listenerOne, listenerTwo);

        assertFilterMulti("SupportBean", statementTwo, new FilterItem[][] {
                {new FilterItem("intPrimitive", FilterOperator.IN_LIST_OF_VALUES)},
        });

        statementOne.destroy();
        statementTwo.destroy();
    }

    private void assertInKeywordReceivedContext(SupportUpdateListener listenerOne, SupportUpdateListener listenerTwo) {
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        assertTrue(listenerOne.getIsInvokedAndReset() && listenerTwo.getIsInvokedAndReset());

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        assertTrue(listenerOne.getIsInvokedAndReset() && listenerTwo.getIsInvokedAndReset());

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        assertFalse(listenerOne.getIsInvokedAndReset() || listenerTwo.getIsInvokedAndReset());
    }

    private void runAssertionHint() {
        String epl = "@Hint('MAX_FILTER_WIDTH=0') select * from SupportBean_IntAlphabetic((b=1 or c=1) and (d=1 or e=1))";
        assertFilterSingle(epl, ".boolean_expression", FilterOperator.BOOLEAN_EXPRESSION);
    }

    private void runAssertionSubquery() {
        String epl = "select (select * from SupportBean_IntAlphabetic(a=1 or b=1)#keepall) as c0 from SupportBean";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        SupportBean_IntAlphabetic iaOne = intEvent(1, 1);
        epService.getEPRuntime().sendEvent(iaOne);
        epService.getEPRuntime().sendEvent(new SupportBean());
        assertEquals(iaOne, listener.assertOneGetNewAndReset().get("c0"));

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionContextPartitionedCategory() {
        epService.getEPAdministrator().createEPL("create context MyContext \n" +
                "  group a=1 or b=1 as g1,\n" +
                "  group c=1 as g1\n" +
                "  from SupportBean_IntAlphabetic");
        String epl = "context MyContext select * from SupportBean_IntAlphabetic(d=1 or e=1)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendAssertEvents(
                new Object[] {intEvent(1, 0, 0, 0, 1), intEvent(0, 1, 0, 1, 0), intEvent(0, 0, 1, 1, 1)},
                new Object[] {intEvent(0, 0, 0, 1, 0), intEvent(1, 0, 0, 0, 0), intEvent(0, 0, 1, 0, 0)}
        );
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionContextPartitionedHash() {
        epService.getEPAdministrator().createEPL("create context MyContext " +
                "coalesce by consistent_hash_crc32(a) from SupportBean_IntAlphabetic(b=1) granularity 16 preallocate");
        String epl = "context MyContext select * from SupportBean_IntAlphabetic(c=1 or d=1)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendAssertEvents(
                new Object[] {intEvent(100, 1, 0, 1), intEvent(100, 1, 1, 0)},
                new Object[] {intEvent(100, 0, 0, 1), intEvent(100, 1, 0, 0)}
        );
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionContextPartitionedSegmented() {
        epService.getEPAdministrator().createEPL("create context MyContext partition by a from SupportBean_IntAlphabetic(b=1 or c=1)");
        String epl = "context MyContext select * from SupportBean_IntAlphabetic(d=1)";
        EPStatement stmt = epService.getEPAdministrator().createEPL(epl);
        stmt.addListener(listener);

        sendAssertEvents(
                new Object[] {intEvent(100, 1, 0, 1), intEvent(100, 0, 1, 1)},
                new Object[] {intEvent(100, 0, 0, 1), intEvent(100, 1, 0, 0)}
        );
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionBooleanExprAnd() {
        String[] filters = new String[] {
                "(a='a' or a like 'A%') and (b='b' or b like 'B%')",
        };
        for (String filter : filters) {
            String epl = "select * from SupportBean_StringAlphabetic(" + filter + ")";
            EPStatement stmt = assertFilterMulti("SupportBean_StringAlphabetic", epl, new FilterItem[][] {
                    {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("b", FilterOperator.EQUAL)},
                    {new FilterItem("a", FilterOperator.EQUAL), getBoolExprFilterItem()},
                    {new FilterItem("b", FilterOperator.EQUAL), getBoolExprFilterItem()},
                    {getBoolExprFilterItem()},
            });
            stmt.addListener(listener);

            sendAssertEvents(
                    new Object[] {stringEvent("a", "b"), stringEvent("A1", "b"), stringEvent("a", "B1"), stringEvent("A1", "B1")},
                    new Object[] {stringEvent("x", "b"), stringEvent("a", "x"), stringEvent("A1", "C"), stringEvent("C", "B1")}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void runAssertionBooleanExprSimple() {
        String[] filters = new String[] {
                "a like 'a%' and (b='b' or c='c')",
        };
        for (String filter : filters) {
            String epl = "select * from SupportBean_StringAlphabetic(" + filter + ")";
            EPStatement stmt = assertFilterMulti("SupportBean_StringAlphabetic", epl, new FilterItem[][] {
                    {new FilterItem("b", FilterOperator.EQUAL), getBoolExprFilterItem()},
                    {new FilterItem("c", FilterOperator.EQUAL), getBoolExprFilterItem()},
            });
            stmt.addListener(listener);

            sendAssertEvents(
                    new Object[] {stringEvent("a1", "b", null), stringEvent("a1", null, "c")},
                    new Object[] {stringEvent("x", "b", null), stringEvent("a1", null, null), stringEvent("a1", null, "x")}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void runAssertionAndRewriteNotEquals() {
        runAssertionAndRewriteNotEqualsOr();

        runAssertionAndRewriteNotEqualsConsolidate();

        runAssertionAndRewriteNotEqualsWithOrConsolidateSecond();
    }

    private void runAssertionAndRewriteNotEqualsWithOrConsolidateSecond() {
        String[] filters = new String[] {
                "a!=1 and a!=2 and ((a!=3 and a!=4) or (a!=5 and a!=6))",
        };
        for (String filter : filters) {
            String epl = "select * from SupportBean_IntAlphabetic(" + filter + ")";
            EPStatement stmt = assertFilterMulti("SupportBean_IntAlphabetic", epl, new FilterItem[][] {
                    {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), getBoolExprFilterItem()},
                    {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), getBoolExprFilterItem()},
            });
            stmt.addListener(listener);

            sendAssertEvents(
                    new Object[] {intEvent(3), intEvent(4), intEvent(0)},
                    new Object[] {intEvent(2), intEvent(1)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void runAssertionAndRewriteNotEqualsConsolidate() {
        String[] filters = new String[] {
                "a!=1 and a!=2 and (a!=3 or a!=4)",
        };
        for (String filter : filters) {
            String epl = "select * from SupportBean_IntAlphabetic(" + filter + ")";
            EPStatement stmt = assertFilterMulti("SupportBean_IntAlphabetic", epl, new FilterItem[][] {
                    {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new FilterItem("a", FilterOperator.NOT_EQUAL)},
                    {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new FilterItem("a", FilterOperator.NOT_EQUAL)},
            });
            stmt.addListener(listener);

            sendAssertEvents(
                    new Object[] {intEvent(3), intEvent(4), intEvent(0)},
                    new Object[] {intEvent(2), intEvent(1)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void runAssertionAndRewriteNotEqualsOr() {
        String[] filters = new String[] {
                "a!=1 and a!=2 and (b=1 or c=1)",
        };
        for (String filter : filters) {
            String epl = "select * from SupportBean_IntAlphabetic(" + filter + ")";
            EPStatement stmt = assertFilterMulti("SupportBean_IntAlphabetic", epl, new FilterItem[][] {
                    {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new FilterItem("b", FilterOperator.EQUAL)},
                    {new FilterItem("a", FilterOperator.NOT_IN_LIST_OF_VALUES), new FilterItem("c", FilterOperator.EQUAL)},
            });
            stmt.addListener(listener);

            sendAssertEvents(
                    new Object[] {intEvent(3, 1, 0), intEvent(3, 0, 1), intEvent(0, 1, 0)},
                    new Object[] {intEvent(2, 0, 0), intEvent(1, 0, 0), intEvent(3, 0, 0)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void runAssertionAndRewriteInnerOr() {
        String[] filtersAB = new String[] {
                "theString='a' and (intPrimitive=1 or longPrimitive=10)",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = assertFilterMulti("SupportBean", epl, new FilterItem[][] {
                    {new FilterItem("theString", FilterOperator.EQUAL), new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("theString", FilterOperator.EQUAL), new FilterItem("longPrimitive", FilterOperator.EQUAL)},
            });
            stmt.addListener(listener);

            sendAssertEvents(
                    new SupportBean[] {makeEvent("a", 1, 0), makeEvent("a", 0, 10), makeEvent("a", 1, 10)},
                    new SupportBean[] {makeEvent("x", 0, 0), makeEvent("a", 2, 20), makeEvent("x", 1, 10)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void runAssertionOrRewriteAndOrMulti() {
        String[] filtersAB = new String[] {
                "a=1 and (b=1 or c=1) and (d=1 or e=1)",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean_IntAlphabetic(" + filter + ")";
            EPStatement stmt = assertFilterMulti("SupportBean_IntAlphabetic", epl, new FilterItem[][] {
                    {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("b", FilterOperator.EQUAL), new FilterItem("d", FilterOperator.EQUAL)},
                    {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("c", FilterOperator.EQUAL), new FilterItem("d", FilterOperator.EQUAL)},
                    {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("c", FilterOperator.EQUAL), new FilterItem("e", FilterOperator.EQUAL)},
                    {new FilterItem("a", FilterOperator.EQUAL), new FilterItem("b", FilterOperator.EQUAL), new FilterItem("e", FilterOperator.EQUAL)},
            });
            stmt.addListener(listener);

            sendAssertEvents(
                    new Object[] {intEvent(1, 1, 0, 1, 0), intEvent(1, 0, 1, 0, 1), intEvent(1, 1, 0, 0, 1), intEvent(1, 0, 1, 1, 0)},
                    new Object[] {intEvent(1, 0, 0, 1, 0), intEvent(1, 0, 0, 1, 0), intEvent(1, 1, 1, 0, 0), intEvent(0, 1, 1, 1, 1)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void runAssertionOrRewriteEightOr() {
        String[] filtersAB = new String[] {
                "theString = 'a' or intPrimitive=1 or longPrimitive=10 or doublePrimitive=100 or boolPrimitive=true or " +
                        "intBoxed=2 or longBoxed=20 or doubleBoxed=200",
                "longBoxed=20 or theString = 'a' or boolPrimitive=true or intBoxed=2 or longPrimitive=10 or doublePrimitive=100 or " +
                        "intPrimitive=1 or doubleBoxed=200",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = assertFilterMulti("SupportBean", epl, new FilterItem[][] {
                    {new FilterItem("theString", FilterOperator.EQUAL)},
                    {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("longPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("doublePrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("boolPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("intBoxed", FilterOperator.EQUAL)},
                    {new FilterItem("longBoxed", FilterOperator.EQUAL)},
                    {new FilterItem("doubleBoxed", FilterOperator.EQUAL)},
            });
            stmt.addListener(listener);

            sendAssertEvents(
                    new SupportBean[] {makeEvent("a", 1, 10, 100, true, 2, 20, 200), makeEvent("a", 0, 0, 0, true, 0, 0, 0),
                            makeEvent("a", 0, 0, 0, true, 0, 20, 0), makeEvent("x", 0, 0, 100, false, 0, 0, 0),
                            makeEvent("x", 1, 0, 0, false, 0, 0, 200), makeEvent("x", 0, 0, 0, false, 0, 0, 200),
                    },
                    new SupportBean[] {makeEvent("x", 0, 0, 0, false, 0, 0, 0)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void runAssertionOrRewriteFourOr() {
        String[] filtersAB = new String[] {
                "theString = 'a' or intPrimitive=1 or longPrimitive=10 or doublePrimitive=100",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = assertFilterMulti("SupportBean", epl, new FilterItem[][] {
                    {new FilterItem("theString", FilterOperator.EQUAL)},
                    {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("longPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("doublePrimitive", FilterOperator.EQUAL)},
            });
            stmt.addListener(listener);

            sendAssertEvents(
                    new SupportBean[] {makeEvent("a", 1, 10, 100), makeEvent("x", 0, 0, 100), makeEvent("x", 0, 10, 100), makeEvent("a", 0, 0, 0)},
                    new SupportBean[] {makeEvent("x", 0, 0, 0)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void runAssertionOrRewriteThreeWithOverlap() {
        String[] filtersAB = new String[] {
                "theString = 'a' or theString = 'b' or intPrimitive=1",
                "intPrimitive = 1 or theString = 'b' or theString = 'a'",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = assertFilterMulti("SupportBean", epl, new FilterItem[][] {
                    {new FilterItem("theString", FilterOperator.EQUAL)},
                    {new FilterItem("theString", FilterOperator.EQUAL)},
                    {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
            });
            stmt.addListener(listener);

            sendAssertEvents(
                    new SupportBean[] {makeEvent("a", 1), makeEvent("b", 0), makeEvent("x", 1)},
                    new SupportBean[] {makeEvent("x", 0)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void runAssertionOrRewriteWithAnd() {
        String[] filtersAB = new String[] {
                "(theString = 'a' and intPrimitive = 1) or (theString = 'b' and intPrimitive = 2)",
                "(intPrimitive = 1 and theString = 'a') or (intPrimitive = 2 and theString = 'b')",
                "(theString = 'b' and intPrimitive = 2) or (theString = 'a' and intPrimitive = 1)",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = assertFilterMulti("SupportBean", epl, new FilterItem[][] {
                    {new FilterItem("theString", FilterOperator.EQUAL), new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("theString", FilterOperator.EQUAL), new FilterItem("intPrimitive", FilterOperator.EQUAL)},
            });
            stmt.addListener(listener);

            sendAssertEvents(
                    new SupportBean[] {makeEvent("a", 1), makeEvent("b", 2)},
                    new SupportBean[] {makeEvent("x", 0), makeEvent("a", 0), makeEvent("a", 2), makeEvent("b", 1)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void runAssertionOrRewriteThreeOr() {
        String[] filtersAB = new String[] {
                "theString = 'a' or intPrimitive = 1 or longPrimitive = 2",
                "2 = longPrimitive or 1 = intPrimitive or theString = 'a'"
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = assertFilterMulti("SupportBean", epl, new FilterItem[][] {
                    {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("theString", FilterOperator.EQUAL)},
                    {new FilterItem("longPrimitive", FilterOperator.EQUAL)},
            });
            stmt.addListener(listener);

            sendAssertEvents(
                    new SupportBean[] {makeEvent("a", 0, 0), makeEvent("b", 1, 0), makeEvent("c", 0, 2), makeEvent("c", 0, 2)},
                    new SupportBean[] {makeEvent("v", 0, 0), makeEvent("c", 2, 1)}
            );
            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private void sendAssertEvents(Object[] matches, Object[] nonMatches) {
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

    private void runAssertionOrRewriteTwoOr() {
        // test 'or' rewrite
        String[] filtersAB = new String[] {
                "theString = 'a' or intPrimitive = 1",
                "theString = 'a' or 1 = intPrimitive",
                "'a' = theString or 1 = intPrimitive",
                "'a' = theString or intPrimitive = 1",
        };
        for (String filter : filtersAB) {
            String epl = "select * from SupportBean(" + filter + ")";
            EPStatement stmt = assertFilterMulti("SupportBean", epl, new FilterItem[][] {
                    {new FilterItem("intPrimitive", FilterOperator.EQUAL)},
                    {new FilterItem("theString", FilterOperator.EQUAL)},
            });
            stmt.addListener(listener);

            epService.getEPRuntime().sendEvent(new SupportBean("a", 0));
            assertTrue(listener.getAndClearIsInvoked());
            epService.getEPRuntime().sendEvent(new SupportBean("b", 1));
            assertTrue(listener.getAndClearIsInvoked());
            epService.getEPRuntime().sendEvent(new SupportBean("c", 0));
            assertFalse(listener.getAndClearIsInvoked());

            epService.getEPAdministrator().destroyAllStatements();
        }
    }

    private EPStatement assertFilterMulti(String eventTypeName, String epl, FilterItem[][] expected) {
        EPStatementSPI statementSPI = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        if (!((FilterServiceSPI) statementSPI.getStatementContext().getFilterService()).isSupportsTakeApply()) {
            return statementSPI;
        }
        assertFilterMulti(eventTypeName, statementSPI, expected);
        return statementSPI;
    }

    private void assertFilterMulti(String eventTypeName, EPStatementSPI statementSPI, FilterItem[][] expected) {
        FilterServiceSPI filterServiceSPI = (FilterServiceSPI) statementSPI.getStatementContext().getFilterService();
        FilterSet set = filterServiceSPI.take(Collections.singleton(statementSPI.getStatementId()));

        FilterSetEntry filterSetEntry = null;
        for (FilterSetEntry entry : set.getFilters()) {
            if (entry.getFilterValueSet().getEventType().getName().equals(eventTypeName)) {
                if (filterSetEntry != null) {
                    fail("Multiple filters for type " + eventTypeName);
                }
                filterSetEntry = entry;
            }
        }

        FilterValueSet valueSet = filterSetEntry.getFilterValueSet();
        FilterValueSetParam[][] params = valueSet.getParameters();

        Comparator<FilterItem> comparator = new Comparator<FilterItem>() {
            public int compare(FilterItem o1, FilterItem o2) {
                if (o1.getName().equals(o2.getName())) {
                    if (o1.getOp().ordinal() > o1.getOp().ordinal()) {
                        return 1;
                    }
                    if (o1.getOp().ordinal() < o1.getOp().ordinal()) {
                        return -1;
                    }
                    return 0;
                }
                return o1.getName().compareTo(o2.getName());
            }
        };

        FilterItem[][] found = new FilterItem[params.length][];
        for (int i = 0; i < found.length; i++) {
            found[i] = new FilterItem[params[i].length];
            for (int j = 0; j < params[i].length; j++) {
                found[i][j] = new FilterItem(params[i][j].getLookupable().getExpression().toString(),
                        params[i][j].getFilterOperator());
            }
            Arrays.sort(found[i], comparator);
        }

        for (int i = 0; i < expected.length; i++) {
            Arrays.sort(expected[i], comparator);
        }

        EPAssertionUtil.assertEqualsAnyOrder(expected, found);
        filterServiceSPI.apply(set);
    }

    private void runAssertionEquals(String epl, SupportUpdateListener[] listeners) {

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

    private void runAssertionBoolean(String epl) {

        // test function returns lookup value and "equals"
        int count = 10;
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
            }
            else {
                assertFalse(listener.isInvoked());
            }
        }
        long delta = System.currentTimeMillis() - startTime;
        assertEquals(loops, MyLib.getCountInvoked());

        log.info("Boolean delta=" + delta);
        assertTrue("Delta is " + delta, delta < 1000);
        epService.getEPAdministrator().destroyAllStatements();
    }

    private void assertFilterSingle(String epl, String expression, FilterOperator op) {
        EPStatementSPI statementSPI = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        if (((FilterServiceSPI) statementSPI.getStatementContext().getFilterService()).isSupportsTakeApply()) {
            FilterValueSetParam param = getFilterSingle(statementSPI);
            assertEquals("failed for '" + epl + "'", op, param.getFilterOperator());
            assertEquals(expression, param.getLookupable().getExpression());
        }
    }

    private void runAssertionTypeOf() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportOverrideBase.class);
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from SupportOverrideBase(typeof(e) = 'SupportOverrideBase') as e");
        stmt.addListener(listener);
        listener.reset();

        epService.getEPRuntime().sendEvent(new SupportOverrideBase(""));
        assertTrue(listener.getAndClearIsInvoked());

        epService.getEPRuntime().sendEvent(new SupportOverrideOne("a", "b"));
        assertFalse(listener.getAndClearIsInvoked());

        stmt.destroy();
    }

    private void assertFilterTwo(String epl, String expressionOne, FilterOperator opOne, String expressionTwo, FilterOperator opTwo) {
        EPStatementSPI statementSPI = (EPStatementSPI) epService.getEPAdministrator().createEPL(epl);
        if (((FilterServiceSPI) statementSPI.getStatementContext().getFilterService()).isSupportsTakeApply()) {
            FilterValueSetParam[] multi = getFilterMulti(statementSPI);
            assertEquals(2, multi.length);
            assertEquals(opOne, multi[0].getFilterOperator());
            assertEquals(expressionOne, multi[0].getLookupable().getExpression());
            assertEquals(opTwo, multi[1].getFilterOperator());
            assertEquals(expressionTwo, multi[1].getLookupable().getExpression());
        }
    }

    private FilterValueSetParam getFilterSingle(EPStatementSPI statementSPI) {
        FilterValueSetParam[] params = getFilterMulti(statementSPI);
        assertEquals(1, params.length);
        return params[0];
    }

    private FilterValueSetParam[] getFilterMulti(EPStatementSPI statementSPI) {
        FilterServiceSPI filterServiceSPI = (FilterServiceSPI) statementSPI.getStatementContext().getFilterService();
        FilterSet set = filterServiceSPI.take(Collections.singleton(statementSPI.getStatementId()));
        assertEquals(1, set.getFilters().size());
        FilterValueSet valueSet = set.getFilters().get(0).getFilterValueSet();
        return valueSet.getParameters()[0];
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
        return new SupportBean_StringAlphabetic(a,b);
    }

    private SupportBean_StringAlphabetic stringEvent(String a, String b, String c) {
        return new SupportBean_StringAlphabetic(a,b,c);
    }

    private SupportBean_IntAlphabetic intEvent(int a, int b, int c) {
        return new SupportBean_IntAlphabetic(a,b,c);
    }

    private SupportBean_IntAlphabetic intEvent(int a, int b, int c, int d, int e) {
        return new SupportBean_IntAlphabetic(a,b,c,d,e);
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

    public FilterItem getBoolExprFilterItem() {
        return new FilterItem(FilterSpecCompiler.PROPERTY_NAME_BOOLEAN_EXPRESSION, FilterOperator.BOOLEAN_EXPRESSION);
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

    public static class FilterItem {
        private final String name;
        private final FilterOperator op;

        public FilterItem(String name, FilterOperator op) {
            this.name = name;
            this.op = op;
        }

        public String getName() {
            return name;
        }

        public FilterOperator getOp() {
            return op;
        }

        public String toString() {
            return "FilterItem{" +
                    "name='" + name + '\'' +
                    ", op=" + op +
                    '}';
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FilterItem that = (FilterItem) o;

            if (op != that.op) return false;
            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
        }

        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (op != null ? op.hashCode() : 0);
            return result;
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
}
