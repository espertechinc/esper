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
package com.espertech.esper.regressionlib.suite.expr.exprcore;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.avro.support.SupportAvroUtil;
import com.espertech.esper.common.internal.support.EventRepresentationChoice;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ExprCoreArray {

    // for use in testing a static method accepting array parameters
    private static Integer[] callbackInts;
    private static String[] callbackStrings;
    private static Object[] callbackObjects;

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> executions = new ArrayList<>();
        executions.add(new ExprCoreArraySimple());
        executions.add(new ExprCoreArrayMapResult());
        executions.add(new ExprCoreArrayCompile());
        executions.add(new ExprCoreArrayExpressionsOM());
        executions.add(new ExprCoreArrayComplexTypes());
        executions.add(new ExprCoreArrayAvroArray());
        return executions;
    }

    private static class ExprCoreArraySimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select {1, 2} as c0 from SupportBean";
            env.compileDeployAddListenerMileZero(epl, "s0");

            env.sendEventBean(new SupportBean());
            Object result = env.listener("s0").assertOneGetNewAndReset().get("c0");
            assertEquals(Integer[].class, result.getClass());
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{1, 2}, (Integer[]) result);

            env.undeployAll();
        }
    }

    private static class ExprCoreArrayMapResult implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select {'a', 'b'} as stringArray," +
                "{} as emptyArray," +
                "{1} as oneEleArray," +
                "{1,2,3} as intArray," +
                "{1,null} as intNullArray," +
                "{1L,10L} as longArray," +
                "{'a',1, 1e20} as mixedArray," +
                "{1, 1.1d, 1e20} as doubleArray," +
                "{5, 6L} as intLongArray," +
                "{null} as nullArray," +
                ExprCoreArray.class.getName() + ".doIt({'a'}, {1}, {1, 'd', null, true}) as func," +
                "{true, false} as boolArray," +
                "{intPrimitive} as dynIntArr," +
                "{intPrimitive, longPrimitive} as dynLongArr," +
                "{intPrimitive, theString} as dynMixedArr," +
                "{intPrimitive, intPrimitive * 2, intPrimitive * 3} as dynCalcArr," +
                "{longBoxed, doubleBoxed * 2, theString || 'a'} as dynCalcArrNulls" +
                " from " + SupportBean.class.getSimpleName();
            env.compileDeploy(epl).addListener("s0");

            SupportBean bean = new SupportBean("a", 10);
            bean.setLongPrimitive(999);
            env.sendEventBean(bean);

            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
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

            env.undeployAll();
        }
    }

    private static class ExprCoreArrayCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select {\"a\",\"b\"} as stringArray, " +
                "{} as emptyArray, " +
                "{1} as oneEleArray, " +
                "{1,2,3} as intArray " +
                "from SupportBean";
            env.eplToModelCompileDeploy(epl).addListener("s0").milestone(0);

            SupportBean bean = new SupportBean("a", 10);
            env.sendEventBean(bean);

            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertEqualsExactOrder((String[]) theEvent.get("stringArray"), new String[]{"a", "b"});
            EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("emptyArray"), new Object[0]);
            EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("oneEleArray"), new Integer[]{1});
            EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("intArray"), new Integer[]{1, 2, 3});

            env.undeployAll();
        }
    }

    private static class ExprCoreArrayComplexTypes implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select {arrayProperty, nested} as field from " + SupportBeanComplexProps.class.getSimpleName();
            env.compileDeploy(epl).addListener("s0");

            SupportBeanComplexProps bean = SupportBeanComplexProps.makeDefaultBean();
            env.sendEventBean(bean);

            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            Object[] arr = (Object[]) theEvent.get("field");
            assertSame(bean.getArrayProperty(), arr[0]);
            assertSame(bean.getNested(), arr[1]);

            env.undeployAll();
        }
    }

    private static class ExprCoreArrayExpressionsOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select {\"a\",\"b\"} as stringArray, " +
                "{} as emptyArray, " +
                "{1} as oneEleArray, " +
                "{1,2,3} as intArray " +
                "from " + SupportBean.class.getSimpleName();
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            model.setSelectClause(SelectClause.create()
                .add(Expressions.array().add(Expressions.constant("a")).add(Expressions.constant("b")), "stringArray")
                .add(Expressions.array(), "emptyArray")
                .add(Expressions.array().add(Expressions.constant(1)), "oneEleArray")
                .add(Expressions.array().add(Expressions.constant(1)).add(2).add(3), "intArray")
            );
            model.setFromClause(FromClause.create(FilterStream.create(SupportBean.class.getSimpleName())));
            assertEquals(epl, model.toEPL());
            env.compileDeploy(model).addListener("s0");

            SupportBean bean = new SupportBean("a", 10);
            env.sendEventBean(bean);

            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertEqualsExactOrder((String[]) theEvent.get("stringArray"), new String[]{"a", "b"});
            EPAssertionUtil.assertEqualsExactOrder((Object[]) theEvent.get("emptyArray"), new Object[0]);
            EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("oneEleArray"), new Integer[]{1});
            EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("intArray"), new Integer[]{1, 2, 3});

            env.undeployAll();
        }
    }

    private static class ExprCoreArrayAvroArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            Schema intArraySchema = SchemaBuilder.array().items(SchemaBuilder.builder().intType());
            Schema mixedArraySchema = SchemaBuilder.array().items(SchemaBuilder.unionOf().intType().and().stringType().and().doubleType().endUnion());
            Schema nullArraySchema = SchemaBuilder.array().items(SchemaBuilder.builder().nullType());

            String stmtText =
                "@name('s0') @AvroSchemaField(name='emptyArray', schema='" + intArraySchema.toString() + "')" +
                    "@AvroSchemaField(name='mixedArray', schema='" + mixedArraySchema.toString() + "')" +
                    "@AvroSchemaField(name='nullArray', schema='" + nullArraySchema.toString() + "')" +
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
                    " from SupportBean";
            env.compileDeploy(stmtText).addListener("s0");

            env.sendEventBean(new SupportBean());

            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
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

            env.undeployAll();
        }
    }

    // for testing EPL static method call
    private static void compareColl(EventBean event, String property, Object[] expected) {
        Collection col = (Collection) event.get(property);
        EPAssertionUtil.assertEqualsExactOrder(col.toArray(), expected);
    }

    public static String[] doIt(String[] strings, Integer[] ints, Object[] objects) {
        callbackInts = ints;
        callbackStrings = strings;
        callbackObjects = objects;
        return new String[]{"a", "b"};
    }
}
