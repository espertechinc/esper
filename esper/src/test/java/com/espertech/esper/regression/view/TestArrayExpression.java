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

package com.espertech.esper.regression.view;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import junit.framework.TestCase;
import com.espertech.esper.client.*;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.support.bean.SupportBean;
import com.espertech.esper.support.bean.SupportBeanComplexProps;
import com.espertech.esper.support.client.SupportConfigFactory;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.util.SerializableObjectCopier;

public class TestArrayExpression extends TestCase
{
    // for use in testing a static method accepting array parameters
    private static Integer[] callbackInts;
    private static String[] callbackStrings;
    private static Object[] callbackObjects;

    private EPServiceProvider epService;

    protected void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    protected void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testArrayExpressions_OM() throws Exception
    {
        String stmtText = "select {\"a\",\"b\"} as stringArray, " +
                              "{} as emptyArray, " +
                              "{1} as oneEleArray, " +
                              "{1,2,3} as intArray " +
                "from " + SupportBean.class.getName();
        EPStatementObjectModel model = new EPStatementObjectModel();
        model.setSelectClause(SelectClause.create()
                .add(Expressions.array().add(Expressions.constant("a")).add(Expressions.constant("b")), "stringArray")
                .add(Expressions.array(), "emptyArray")
                .add(Expressions.array().add(Expressions.constant(1)), "oneEleArray")
                .add(Expressions.array().add(Expressions.constant(1)).add(2).add(3), "intArray")
                );
        model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getName())));
        model = (EPStatementObjectModel) SerializableObjectCopier.copy(model);
        assertEquals(stmtText, model.toEPL());
        
        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean bean = new SupportBean("a", 10);
        epService.getEPRuntime().sendEvent(bean);

        EventBean theEvent = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertEqualsExactOrder((String[]) theEvent.get("stringArray"), new String[]{"a", "b"});
        EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("emptyArray"), new Object[0]);
        EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("oneEleArray"), new Integer[]{1});
        EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("intArray"), new Integer[]{1, 2, 3});
    }

    public void testArrayExpressions_Compile()
    {
        String stmtText = "select {\"a\",\"b\"} as stringArray, " +
                              "{} as emptyArray, " +
                              "{1} as oneEleArray, " +
                              "{1,2,3} as intArray " +
                "from " + SupportBean.class.getName();
        EPStatementObjectModel model = epService.getEPAdministrator().compileEPL(stmtText);
        assertEquals(stmtText, model.toEPL());
        
        EPStatement stmt = epService.getEPAdministrator().create(model);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean bean = new SupportBean("a", 10);
        epService.getEPRuntime().sendEvent(bean);

        EventBean theEvent = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertEqualsExactOrder((String[]) theEvent.get("stringArray"), new String[]{"a", "b"});
        EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("emptyArray"), new Object[0]);
        EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("oneEleArray"), new Integer[]{1});
        EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("intArray"), new Integer[]{1, 2, 3});
    }

    public void testArrayExpressions()
    {
        String stmtText = "select {'a', 'b'} as stringArray," +
                              "{} as emptyArray," +
                              "{1} as oneEleArray," +
                              "{1,2,3} as intArray," +
                              "{1,null} as intNullArray," +
                              "{1L,10L} as longArray," +
                              "{'a',1, 1e20} as mixedArray," +
                              "{1, 1.1d, 1e20} as doubleArray," +
                              "{5, 6L} as intLongArray," +
                              "{null} as nullArray," +
                              TestArrayExpression.class.getName() + ".doIt({'a'}, {1}, {1, 'd', null, true}) as func," +
                              "{true, false} as boolArray," +
                              "{intPrimitive} as dynIntArr," +
                              "{intPrimitive, longPrimitive} as dynLongArr," +
                              "{intPrimitive, theString} as dynMixedArr," +
                              "{intPrimitive, intPrimitive * 2, intPrimitive * 3} as dynCalcArr," +
                              "{longBoxed, doubleBoxed * 2, theString || 'a'} as dynCalcArrNulls" +
                              " from " + SupportBean.class.getName();

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBean bean = new SupportBean("a", 10);
        bean.setLongPrimitive(999);
        epService.getEPRuntime().sendEvent(bean);

        EventBean theEvent = listener.assertOneGetNewAndReset();
        EPAssertionUtil.assertEqualsExactOrder((String[]) theEvent.get("stringArray"), new String[]{"a", "b"});
        EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("emptyArray"), new Object[0]);
        EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("oneEleArray"), new Integer[]{1});
        EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("intArray"), new Integer[]{1, 2, 3});
        EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("intNullArray"), new Integer[]{1, null});
        EPAssertionUtil.assertEqualsExactOrder((Long[]) theEvent.get("longArray"), new Long[]{1L, 10L});
        EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("mixedArray"), new Object[]{"a", 1, 1e20});
        EPAssertionUtil.assertEqualsExactOrder((Double[]) theEvent.get("doubleArray"), new Double[]{1d, 1.1, 1e20});
        EPAssertionUtil.assertEqualsExactOrder((Long[]) theEvent.get("intLongArray"), new Long[]{5L, 6L});
        EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("nullArray"), new Object[]{null});
        EPAssertionUtil.assertEqualsExactOrder((String[]) theEvent.get("func"), new String[]{"a", "b"});
        EPAssertionUtil.assertEqualsExactOrder((Boolean[]) theEvent.get("boolArray"), new Boolean[]{true, false});
        EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("dynIntArr"), new Integer[]{10});
        EPAssertionUtil.assertEqualsExactOrder((Long[]) theEvent.get("dynLongArr"), new Long[]{10L, 999L});
        EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("dynMixedArr"), new Object[]{10, "a"});
        EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("dynCalcArr"), new Integer[]{10, 20, 30});
        EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("dynCalcArrNulls"), new Object[]{null, null, "aa"});

        // assert function parameters
        EPAssertionUtil.assertEqualsExactOrder(callbackInts, new Integer[]{1});
        EPAssertionUtil.assertEqualsExactOrder(callbackStrings, new String[]{"a"});
        EPAssertionUtil.assertEqualsExactOrder(callbackObjects, new Object[]{1, "d", null, true});
    }

    public void testComplexTypes()
    {
        String stmtText = "select {arrayProperty, nested} as field" +
                              " from " + SupportBeanComplexProps.class.getName();

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        SupportBeanComplexProps bean = SupportBeanComplexProps.makeDefaultBean();
        epService.getEPRuntime().sendEvent(bean);

        EventBean theEvent = listener.assertOneGetNewAndReset();
        Object[] arr = (Object[]) theEvent.get("field");
        assertSame(bean.getArrayProperty(), arr[0]);
        assertSame(bean.getNested(), arr[1]);
    }

    // for testing EPL static method call
    public static String[] doIt(String[] strings, Integer[] ints, Object[] objects)
    {
        callbackInts = ints;
        callbackStrings = strings;
        callbackObjects = objects;
        return new String[] {"a", "b"};
    }
}
