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

import com.espertech.esper.avro.util.support.SupportAvroUtil;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanComplexProps;
import com.espertech.esper.supportregression.client.SupportConfigFactory;
import com.espertech.esper.util.EventRepresentationChoice;
import com.espertech.esper.util.SerializableObjectCopier;
import junit.framework.TestCase;
import org.apache.avro.Schema;

import java.util.Collection;

import static org.apache.avro.SchemaBuilder.*;

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

    public void testArrayExpressions() throws Exception {
        runAssertionArrayMapResult();
        runAssertionArrayAvroResult();
        runAssertionArrayExpressions_Compile();
        runAssertionArrayExpressions_OM();
        runAssertionComplexTypes();
    }

    private void runAssertionComplexTypes()
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

        stmt.destroy();
    }

    private void runAssertionArrayMapResult() {
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

        stmt.destroy();
    }

    private void runAssertionArrayAvroResult() {
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);

        Schema intArraySchema = array().items(builder().intType());
        Schema mixedArraySchema = array().items(unionOf().intType().and().stringType().and().doubleType().endUnion());
        Schema nullArraySchema = array().items(builder().nullType());

        String stmtText =
                "@AvroField(name='emptyArray', schema='" + intArraySchema.toString() + "')" +
                "@AvroField(name='mixedArray', schema='" + mixedArraySchema.toString() + "')" +
                "@AvroField(name='nullArray', schema='" + nullArraySchema.toString() + "')" +
                EventRepresentationChoice.AVRO.getAnnotationText() +
                "select {'a', 'b'} as stringArray," +
                "{} as emptyArray," +
                "{1} as oneEleArray," +
                "{1,2,3} as intArray," +
                "{1,null} as intNullArray," +
                "{1L,10L} as longArray," +
                "{'a',1, 1e20} as mixedArray," +
                "{1, 1.1d, 1e20} as doubleArray," +
                "{5, 6L} as intLongArray," +
                "{null} as nullArray," +
                "{true, false} as boolArray" +
                " from " + SupportBean.class.getName();

        EPStatement stmt = epService.getEPAdministrator().createEPL(stmtText);
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);
        epService.getEPRuntime().sendEvent(new SupportBean());

        EventBean theEvent = listener.assertOneGetNewAndReset();
        SupportAvroUtil.avroToJson(theEvent);

        compareColl(theEvent, "stringArray", new String[]{"a", "b"});
        compareColl(theEvent, "emptyArray", new Object[0]);
        compareColl(theEvent, "oneEleArray", new Integer[]{1});
        compareColl(theEvent, "intArray", new Integer[]{1, 2, 3});
        compareColl(theEvent, "intNullArray", new Integer[]{1, null});
        compareColl(theEvent, "longArray", new Long[]{1L, 10L});
        compareColl(theEvent, "mixedArray", new Object[]{"a", 1, 1e20});
        compareColl(theEvent, "doubleArray", new Double[]{1d, 1.1, 1e20});
        compareColl(theEvent, "intLongArray", new Long[]{5L, 6L});
        compareColl(theEvent, "nullArray", new Object[]{null});
        compareColl(theEvent, "boolArray", new Boolean[]{true, false});

        stmt.destroy();
    }

    private void runAssertionArrayExpressions_OM() throws Exception
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

        stmt.destroy();
    }

    private void runAssertionArrayExpressions_Compile()
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

        stmt.destroy();
    }

    // for testing EPL static method call
    public static String[] doIt(String[] strings, Integer[] ints, Object[] objects)
    {
        callbackInts = ints;
        callbackStrings = strings;
        callbackObjects = objects;
        return new String[] {"a", "b"};
    }

    private static void compareColl(EventBean event, String property, Object[] expected) {
        Collection col = ((Collection) event.get(property));
        EPAssertionUtil.assertEqualsExactOrder(col.toArray(), expected);
    }
}
