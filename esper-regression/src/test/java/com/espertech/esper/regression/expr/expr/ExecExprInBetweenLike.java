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
package com.espertech.esper.regression.expr.expr;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanArrayCollMap;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.util.SerializableObjectCopier;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertNull;

public class ExecExprInBetweenLike implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionInObject(epService);
        runAssertionInArraySubstitution(epService);
        runAssertionInCollection(epService);
        runAssertionInStringExprOM(epService);
        runAssertionInStringExpr(epService);
        runAssertionInNumericExpr(epService);
        runAssertionInBoolExpr(epService);
        runAssertionInNumericCoercionLong(epService);
        runAssertionInNumericCoercionDouble(epService);
        runAssertionBetweenStringExpr(epService);
        runAssertionBetweenBigIntBigDecExpr(epService);
        runAssertionBetweenNumericExpr(epService);
        runAssertionBetweenNumericCoercionLong(epService);
        runAssertionInRange(epService);
        runAssertionBetweenNumericCoercionDouble(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionInObject(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("ArrayBean", SupportBeanArrayCollMap.class);
        String stmtText = "select s0.anyObject in (objectArr) as value from ArrayBean s0";

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean_S1 s1 = new SupportBean_S1(100);
        SupportBeanArrayCollMap arrayBean = new SupportBeanArrayCollMap(s1);
        arrayBean.setObjectArr(new Object[]{null, "a", false, s1});
        epService.getEPRuntime().sendEvent(arrayBean);
        assertEquals(true, listener.assertOneGetNewAndReset().get("value"));

        arrayBean.setAnyObject(null);
        epService.getEPRuntime().sendEvent(arrayBean);
        assertNull(listener.assertOneGetNewAndReset().get("value"));

        stmt.destroy();
    }

    private void runAssertionInArraySubstitution(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        String stmtText = "select intPrimitive in (?) as result from SupportBean";
        EPPreparedStatement prepared = epService.getEPAdministrator().prepareEPL(stmtText);
        prepared.setObject(1, new int[]{10, 20, 30});
        EPStatement stmt = epService.getEPAdministrator().create(prepared);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 10));
        assertTrue((Boolean) listener.assertOneGetNewAndReset().get("result"));

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 9));
        assertFalse((Boolean) listener.assertOneGetNewAndReset().get("result"));

        stmt.destroy();
    }

    private void runAssertionInCollection(EPServiceProvider epService) {
        SupportUpdateListener listener = new SupportUpdateListener();
        String stmtText;
        EPStatement stmt;

        stmtText = "select 10 in (arrayProperty) as result from " + SupportBeanComplexProps.class.getName();
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);
        assertEquals(Boolean.class, stmt.getEventType().getPropertyType("result"));

        stmtText = "select 5 in (arrayProperty) as result from " + SupportBeanComplexProps.class.getName();
        EPStatement selectTestCaseTwo = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listenerTwo = new SupportUpdateListener();
        selectTestCaseTwo.addListener(listenerTwo);

        epService.getEPRuntime().sendEvent(SupportBeanComplexProps.makeDefaultBean());
        assertEquals(true, listener.assertOneGetNewAndReset().get("result"));
        assertEquals(false, listenerTwo.assertOneGetNewAndReset().get("result"));

        stmt.stop();
        selectTestCaseTwo.stop();

        // Arrays
        stmtText = "select 1 in (intArr, longArr) as resOne, 1 not in (intArr, longArr) as resTwo from " + SupportBeanArrayCollMap.class.getName();
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        String[] fields = "resOne, resTwo".split(",");
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(new int[]{10, 20, 30}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(new int[]{10, 1, 30}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(new int[]{30}, new Long[]{20L, 1L}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(new int[]{}, new Long[]{null, 1L}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(null, new Long[]{1L, 100L}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(null, new Long[]{0L, 100L}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});
        stmt.destroy();

        // Collection
        stmtText = "select 1 in (intCol, longCol) as resOne, 1 not in (longCol, intCol) as resTwo from " + SupportBeanArrayCollMap.class.getName();
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(true, new int[]{10, 20, 30}, null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(true, new int[]{10, 20, 1}, null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(true, new int[]{30}, new Long[]{20L, 1L}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(true, new int[]{}, new Long[]{null, 1L}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(true, null, new Long[]{1L, 100L}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});
        stmt.destroy();

        // Maps
        stmtText = "select 1 in (longMap, intMap) as resOne, 1 not in (longMap, intMap) as resTwo from " + SupportBeanArrayCollMap.class.getName();
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(false, new int[]{10, 20, 30}, null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(false, new int[]{10, 20, 1}, null));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(false, new int[]{30}, new Long[]{20L, 1L}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(false, new int[]{}, new Long[]{null, 1L}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(false, null, new Long[]{1L, 100L}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});
        stmt.destroy();

        // Mixed
        stmtText = "select 1 in (longBoxed, intArr, longMap, intCol) as resOne, 1 not in (longBoxed, intArr, longMap, intCol) as resTwo from " + SupportBeanArrayCollMap.class.getName();
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(1L, new int[0], new Long[0], new int[0]));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(2L, null, new Long[0], new int[0]));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});

        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(null, null, null, new int[]{3, 4, 5, 6, 7, 7, 7, 8, 8, 8, 1}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});

        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(-1L, null, new Long[]{1L}, new int[]{3, 4, 5, 6, 7, 7, 7, 8, 8}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(-1L, new int[]{1}, null, new int[]{}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});
        stmt.destroy();

        // Object array
        stmtText = "select 1 in (objectArr) as resOne, 2 in (objectArr) as resTwo from " + SupportBeanArrayCollMap.class.getName();
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(new Object[]{}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(new Object[]{1, 2}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(new Object[]{1d, 2L}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false});
        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(new Object[]{null, 2}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{null, true});
        stmt.destroy();

        // Object array
        stmtText = "select 1 in ({1,2,3}) as resOne, 2 in ({0, 1}) as resTwo from " + SupportBeanArrayCollMap.class.getName();
        stmt = epService.getEPAdministrator().createEPL(stmtText);
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanArrayCollMap(new Object[]{}));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, false});

        stmt.destroy();
    }

    private void runAssertionInStringExprOM(EPServiceProvider epService) throws Exception {
        String caseExpr = "select theString in (\"a\",\"b\",\"c\") as result from " + SupportBean.class.getName();
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.in("theString", "a", "b", "c"), "result"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName())));

        tryString(epService, model, caseExpr,
                new String[]{"0", "a", "b", "c", "d", null},
                new Boolean[]{false, true, true, true, false, null});

        model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create().add(Expressions.notIn("theString", "a", "b", "c"), "result"));
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName())));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);

        tryString(epService, "theString not in ('a', 'b', 'c')",
                new String[]{"0", "a", "b", "c", "d", null},
                new Boolean[]{true, false, false, false, true, null});
    }

    private void runAssertionInStringExpr(EPServiceProvider epService) {
        tryString(epService, "theString in ('a', 'b', 'c')",
                new String[]{"0", "a", "b", "c", "d", null},
                new Boolean[]{false, true, true, true, false, null});

        tryString(epService, "theString in ('a')",
                new String[]{"0", "a", "b", "c", "d", null},
                new Boolean[]{false, true, false, false, false, null});

        tryString(epService, "theString in ('a', 'b')",
                new String[]{"0", "b", "a", "c", "d", null},
                new Boolean[]{false, true, true, false, false, null});

        tryString(epService, "theString in ('a', null)",
                new String[]{"0", "b", "a", "c", "d", null},
                new Boolean[]{null, null, true, null, null, null});

        tryString(epService, "theString in (null)",
                new String[]{"0", null, "b"},
                new Boolean[]{null, null, null});

        tryString(epService, "theString not in ('a', 'b', 'c')",
                new String[]{"0", "a", "b", "c", "d", null},
                new Boolean[]{true, false, false, false, true, null});

        tryString(epService, "theString not in (null)",
                new String[]{"0", null, "b"},
                new Boolean[]{null, null, null});
    }

    private void runAssertionBetweenBigIntBigDecExpr(EPServiceProvider epService) {
        String[] fields = "c0,c1,c2,c3".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL("select " +
                "intPrimitive between BigInteger.valueOf(1) and BigInteger.valueOf(3) as c0," +
                "intPrimitive between BigDecimal.valueOf(1) and BigDecimal.valueOf(3) as c1," +
                "intPrimitive in (BigInteger.valueOf(1):BigInteger.valueOf(3)) as c2," +
                "intPrimitive in (BigDecimal.valueOf(1):BigDecimal.valueOf(3)) as c3" +
                " from SupportBean");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E0", 0));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {false, false, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {true, true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E2", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {true, true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E3", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {true, true, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E4", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[] {false, false, false, false});
    }

    private void runAssertionBetweenStringExpr(EPServiceProvider epService) {
        String[] input = null;
        Boolean[] result = null;

        input = new String[]{"0", "a1", "a10", "c", "d", null, "a0", "b9", "b90"};
        result = new Boolean[]{false, true, true, false, false, false, true, true, false};
        tryString(epService, "theString between 'a0' and 'b9'", input, result);
        tryString(epService, "theString between 'b9' and 'a0'", input, result);

        tryString(epService, "theString between null and 'b9'",
                new String[]{"0", null, "a0", "b9"},
                new Boolean[]{false, false, false, false});

        tryString(epService, "theString between null and null",
                new String[]{"0", null, "a0", "b9"},
                new Boolean[]{false, false, false, false});

        tryString(epService, "theString between 'a0' and null",
                new String[]{"0", null, "a0", "b9"},
                new Boolean[]{false, false, false, false});

        input = new String[]{"0", "a1", "a10", "c", "d", null, "a0", "b9", "b90"};
        result = new Boolean[]{true, false, false, true, true, false, false, false, true};
        tryString(epService, "theString not between 'a0' and 'b9'", input, result);
        tryString(epService, "theString not between 'b9' and 'a0'", input, result);
    }

    private void runAssertionInNumericExpr(EPServiceProvider epService) {
        Double[] input = new Double[]{1d, null, 1.1d, 1.0d, 1.0999999999, 2d, 4d};
        Boolean[] result = new Boolean[]{false, null, true, false, false, true, true};
        tryNumeric(epService, "doubleBoxed in (1.1d, 7/3.5, 2*6/3, 0)", input, result);

        tryNumeric(epService, "doubleBoxed in (7/3d, null)",
                new Double[]{2d, 7 / 3d, null},
                new Boolean[]{null, true, null});

        tryNumeric(epService, "doubleBoxed in (5,5,5,5,5, -1)",
                new Double[]{5.0, 5d, 0d, null, -1d},
                new Boolean[]{true, true, false, null, true});

        tryNumeric(epService, "doubleBoxed not in (1.1d, 7/3.5, 2*6/3, 0)",
                new Double[]{1d, null, 1.1d, 1.0d, 1.0999999999, 2d, 4d},
                new Boolean[]{true, null, false, true, true, false, false});
    }

    private void runAssertionBetweenNumericExpr(EPServiceProvider epService) {
        Double[] input = new Double[]{1d, null, 1.1d, 2d, 1.0999999999, 2d, 4d, 15d, 15.00001d};
        Boolean[] result = new Boolean[]{false, false, true, true, false, true, true, true, false};
        tryNumeric(epService, "doubleBoxed between 1.1 and 15", input, result);
        tryNumeric(epService, "doubleBoxed between 15 and 1.1", input, result);

        tryNumeric(epService, "doubleBoxed between null and 15",
                new Double[]{1d, null, 1.1d},
                new Boolean[]{false, false, false});

        tryNumeric(epService, "doubleBoxed between 15 and null",
                new Double[]{1d, null, 1.1d},
                new Boolean[]{false, false, false});

        tryNumeric(epService, "doubleBoxed between null and null",
                new Double[]{1d, null, 1.1d},
                new Boolean[]{false, false, false});

        input = new Double[]{1d, null, 1.1d, 2d, 1.0999999999, 2d, 4d, 15d, 15.00001d};
        result = new Boolean[]{true, false, false, false, true, false, false, false, true};
        tryNumeric(epService, "doubleBoxed not between 1.1 and 15", input, result);
        tryNumeric(epService, "doubleBoxed not between 15 and 1.1", input, result);

        tryNumeric(epService, "doubleBoxed not between 15 and null",
                new Double[]{1d, null, 1.1d},
                new Boolean[]{false, false, false});
    }

    private void runAssertionInBoolExpr(EPServiceProvider epService) {
        tryInBoolean(epService, "boolBoxed in (true, true)",
                new Boolean[]{true, false},
                new boolean[]{true, false});

        tryInBoolean(epService, "boolBoxed in (1>2, 2=3, 4<=2)",
                new Boolean[]{true, false},
                new boolean[]{false, true});

        tryInBoolean(epService, "boolBoxed not in (1>2, 2=3, 4<=2)",
                new Boolean[]{true, false},
                new boolean[]{true, false});
    }

    private void runAssertionInNumericCoercionLong(EPServiceProvider epService) {
        String caseExpr = "select intPrimitive in (shortBoxed, intBoxed, longBoxed) as result from " + SupportBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        selectTestCase.addListener(listener);
        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("result"));

        sendAndAssert(epService, listener, 1, 2, 3, 4L, false);
        sendAndAssert(epService, listener, 1, 1, 3, 4L, true);
        sendAndAssert(epService, listener, 1, 3, 1, 4L, true);
        sendAndAssert(epService, listener, 1, 3, 7, 1L, true);
        sendAndAssert(epService, listener, 1, 3, 7, null, null);
        sendAndAssert(epService, listener, 1, 1, null, null, true);
        sendAndAssert(epService, listener, 1, 0, null, 1L, true);

        selectTestCase.stop();
    }

    private void runAssertionInNumericCoercionDouble(EPServiceProvider epService) {
        String caseExpr = "select intBoxed in (floatBoxed, doublePrimitive, longBoxed) as result from " + SupportBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        selectTestCase.addListener(listener);
        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("result"));

        sendAndAssert(epService, listener, 1, 2f, 3d, 4L, false);
        sendAndAssert(epService, listener, 1, 1f, 3d, 4L, true);
        sendAndAssert(epService, listener, 1, 1.1f, 1.0d, 4L, true);
        sendAndAssert(epService, listener, 1, 1.1f, 1.2d, 1L, true);
        sendAndAssert(epService, listener, 1, null, 1.2d, 1L, true);
        sendAndAssert(epService, listener, null, null, 1.2d, 1L, null);
        sendAndAssert(epService, listener, null, 11f, 1.2d, 1L, null);

        selectTestCase.stop();
    }

    private void runAssertionBetweenNumericCoercionLong(EPServiceProvider epService) {
        String caseExpr = "select intPrimitive between shortBoxed and longBoxed as result from " + SupportBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        selectTestCase.addListener(listener);
        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("result"));

        sendAndAssert(epService, listener, 1, 2, 3L, false);
        sendAndAssert(epService, listener, 2, 2, 3L, true);
        sendAndAssert(epService, listener, 3, 2, 3L, true);
        sendAndAssert(epService, listener, 4, 2, 3L, false);
        sendAndAssert(epService, listener, 5, 10, 1L, true);
        sendAndAssert(epService, listener, 1, 10, 1L, true);
        sendAndAssert(epService, listener, 10, 10, 1L, true);
        sendAndAssert(epService, listener, 11, 10, 1L, false);

        selectTestCase.stop();
    }

    private void runAssertionInRange(EPServiceProvider epService) {

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);

        String[] fields = "ro,rc,rho,rhc,nro,nrc,nrho,nrhc".split(",");
        EPStatement stmt = epService.getEPAdministrator().createEPL(
                "select intPrimitive in (2:4) as ro, intPrimitive in [2:4] as rc, intPrimitive in [2:4) as rho, intPrimitive in (2:4] as rhc, " +
                        "intPrimitive not in (2:4) as nro, intPrimitive not in [2:4] as nrc, intPrimitive not in [2:4) as nrho, intPrimitive not in (2:4] as nrhc " +
                        "from SupportBean#lastevent");
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false, false, false, true, true, true, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 2));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true, true, false, true, false, false, true});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true, true, true, false, false, false, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 4));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, true, false, true, true, false, true, false});

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false, false, false, false, true, true, true, true});

        // test range reversed
        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL(
                "select intPrimitive between 4 and 2 as r1, intPrimitive in [4:2] as r2 from SupportBean#lastevent");
        stmt.addListener(listener);

        fields = "r1,r2".split(",");
        epService.getEPRuntime().sendEvent(new SupportBean("E1", 3));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true, true});

        // test string type
        stmt.destroy();
        fields = "ro".split(",");
        stmt = epService.getEPAdministrator().createEPL("select theString in ('a':'d') as ro from SupportBean#lastevent");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean("a", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        epService.getEPRuntime().sendEvent(new SupportBean("b", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true});

        epService.getEPRuntime().sendEvent(new SupportBean("c", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{true});

        epService.getEPRuntime().sendEvent(new SupportBean("d", 5));
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), fields, new Object[]{false});

        stmt.destroy();
    }

    private void runAssertionBetweenNumericCoercionDouble(EPServiceProvider epService) {
        String caseExpr = "select intBoxed between floatBoxed and doublePrimitive as result from " + SupportBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        selectTestCase.addListener(listener);
        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("result"));

        sendAndAssert(epService, listener, 1, 2f, 3d, false);
        sendAndAssert(epService, listener, 2, 2f, 3d, true);
        sendAndAssert(epService, listener, 3, 2f, 3d, true);
        sendAndAssert(epService, listener, 4, 2f, 3d, false);
        sendAndAssert(epService, listener, null, 2f, 3d, false);
        sendAndAssert(epService, listener, null, null, 3d, false);
        sendAndAssert(epService, listener, 1, 3f, 2d, false);
        sendAndAssert(epService, listener, 2, 3f, 2d, true);
        sendAndAssert(epService, listener, 3, 3f, 2d, true);
        sendAndAssert(epService, listener, 4, 3f, 2d, false);
        sendAndAssert(epService, listener, null, 3f, 2d, false);
        sendAndAssert(epService, listener, null, null, 2d, false);

        selectTestCase.stop();
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        epService.getEPAdministrator().getConfiguration().addEventType("ArrayBean", SupportBeanArrayCollMap.class);
        try {
            String stmtText = "select intArr in (1, 2, 3) as r1 from ArrayBean";
            epService.getEPAdministrator().createEPL(stmtText);
            fail();
        } catch (EPStatementException ex) {
            assertEquals("Error starting statement: Failed to validate select-clause expression 'intArr in (1,2,3)': Collection or array comparison is not allowed for the IN, ANY, SOME or ALL keywords [select intArr in (1, 2, 3) as r1 from ArrayBean]", ex.getMessage());
        }
    }

    private void sendAndAssert(EPServiceProvider epService, SupportUpdateListener listener, Integer intBoxed, Float floatBoxed, double doublePrimitive, Boolean result) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setFloatBoxed(floatBoxed);
        bean.setDoublePrimitive(doublePrimitive);

        epService.getEPRuntime().sendEvent(bean);

        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(result, theEvent.get("result"));
    }

    private void sendAndAssert(EPServiceProvider epService, SupportUpdateListener listener, int intPrimitive, int shortBoxed, Integer intBoxed, Long longBoxed, Boolean result) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setShortBoxed((short) shortBoxed);
        bean.setIntBoxed(intBoxed);
        bean.setLongBoxed(longBoxed);

        epService.getEPRuntime().sendEvent(bean);

        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(result, theEvent.get("result"));
    }

    private void sendAndAssert(EPServiceProvider epService, SupportUpdateListener listener, int intPrimitive, int shortBoxed, Long longBoxed, Boolean result) {
        SupportBean bean = new SupportBean();
        bean.setIntPrimitive(intPrimitive);
        bean.setShortBoxed((short) shortBoxed);
        bean.setLongBoxed(longBoxed);

        epService.getEPRuntime().sendEvent(bean);

        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(result, theEvent.get("result"));
    }

    private void sendAndAssert(EPServiceProvider epService, SupportUpdateListener listener, Integer intBoxed, Float floatBoxed, double doublePrimitve, Long longBoxed, Boolean result) {
        SupportBean bean = new SupportBean();
        bean.setIntBoxed(intBoxed);
        bean.setFloatBoxed(floatBoxed);
        bean.setDoublePrimitive(doublePrimitve);
        bean.setLongBoxed(longBoxed);

        epService.getEPRuntime().sendEvent(bean);

        EventBean theEvent = listener.assertOneGetNewAndReset();
        assertEquals(result, theEvent.get("result"));
    }

    private void tryInBoolean(EPServiceProvider epService, String expr, Boolean[] input, boolean[] result) {
        String caseExpr = "select " + expr + " as result from " + SupportBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        selectTestCase.addListener(listener);
        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("result"));

        for (int i = 0; i < input.length; i++) {
            sendSupportBeanEvent(epService, input[i]);
            EventBean theEvent = listener.assertOneGetNewAndReset();
            assertEquals("Wrong result for " + input[i], result[i], theEvent.get("result"));
        }
        selectTestCase.stop();
    }

    private void tryNumeric(EPServiceProvider epService, String expr, Double[] input, Boolean[] result) {
        String caseExpr = "select " + expr + " as result from " + SupportBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        selectTestCase.addListener(listener);
        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("result"));

        for (int i = 0; i < input.length; i++) {
            sendSupportBeanEvent(epService, input[i]);
            EventBean theEvent = listener.assertOneGetNewAndReset();
            assertEquals("Wrong result for " + input[i], result[i], theEvent.get("result"));
        }
        selectTestCase.stop();
    }

    private void tryString(EPServiceProvider epService, String expression, String[] input, Boolean[] result) {
        String caseExpr = "select " + expression + " as result from " + SupportBean.class.getName();

        EPStatement selectTestCase = epService.getEPAdministrator().createEPL(caseExpr);
        SupportUpdateListener listener = new SupportUpdateListener();
        selectTestCase.addListener(listener);
        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("result"));

        for (int i = 0; i < input.length; i++) {
            sendSupportBeanEvent(epService, input[i]);
            EventBean theEvent = listener.assertOneGetNewAndReset();
            assertEquals("Wrong result for " + input[i], result[i], theEvent.get("result"));
        }
        selectTestCase.stop();
    }

    private void tryString(EPServiceProvider epService, EPStatementObjectModel model, String epl, String[] input, Boolean[] result) throws Exception {
        EPStatement selectTestCase = epService.getEPAdministrator().create(model);
        assertEquals(epl, model.toEPL());

        EPStatementObjectModel compiled = epService.getEPAdministrator().compileEPL(epl);
        compiled = (EPStatementObjectModel) SerializableObjectCopier.copy(compiled);
        assertEquals(epl, compiled.toEPL());

        SupportUpdateListener listener = new SupportUpdateListener();
        selectTestCase.addListener(listener);
        assertEquals(Boolean.class, selectTestCase.getEventType().getPropertyType("result"));

        for (int i = 0; i < input.length; i++) {
            sendSupportBeanEvent(epService, input[i]);
            EventBean theEvent = listener.assertOneGetNewAndReset();
            assertEquals("Wrong result for " + input[i], result[i], theEvent.get("result"));
        }
        selectTestCase.stop();
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, Double doubleBoxed) {
        SupportBean theEvent = new SupportBean();
        theEvent.setDoubleBoxed(doubleBoxed);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, String theString) {
        SupportBean theEvent = new SupportBean();
        theEvent.setTheString(theString);
        epService.getEPRuntime().sendEvent(theEvent);
    }

    private void sendSupportBeanEvent(EPServiceProvider epService, boolean boolBoxed) {
        SupportBean theEvent = new SupportBean();
        theEvent.setBoolBoxed(boolBoxed);
        epService.getEPRuntime().sendEvent(theEvent);
    }
}
