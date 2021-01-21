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
package com.espertech.esper.regressionlib.suite.epl.other;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.support.SupportEventPropDesc;
import com.espertech.esper.common.internal.support.SupportEventPropUtil;
import com.espertech.esper.common.internal.util.SerializableObjectCopier;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;

import java.util.*;

import static org.junit.Assert.*;

public class EPLOtherSelectWildcardWAdditional {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherSingleOM());
        execs.add(new EPLOtherSingle());
        execs.add(new EPLOtherSingleInsertInto());
        execs.add(new EPLOtherJoinInsertInto());
        execs.add(new EPLOtherJoinNoCommonProperties());
        execs.add(new EPLOtherJoinCommonProperties());
        execs.add(new EPLOtherCombinedProperties());
        execs.add(new EPLOtherWildcardMapEvent());
        execs.add(new EPLOtherInvalidRepeatedProperties());
        return execs;
    }

    private static class EPLOtherSingleOM implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.createWildcard().add(Expressions.concat("myString", "myString"), "concat"));
            model.setFromClause(FromClause.create(FilterStream.create("SupportBeanSimple").addView(View.create("length", Expressions.constant(5)))));
            model = SerializableObjectCopier.copyMayFail(model);

            String text = "select *, myString||myString as concat from SupportBeanSimple#length(5)";
            assertEquals(text, model.toEPL());
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            assertSimple(env);

            env.assertStatement("s0", statement -> SupportEventPropUtil.assertPropsEquals(statement.getEventType().getPropertyDescriptors(),
                new SupportEventPropDesc("myString", String.class),
                new SupportEventPropDesc("myInt", int.class),
                new SupportEventPropDesc("concat", String.class)));

            env.undeployAll();
        }
    }

    private static class EPLOtherSingle implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select *, myString||myString as concat from SupportBeanSimple#length(5)";
            env.compileDeploy(text).addListener("s0");
            assertSimple(env);
            env.undeployAll();
        }
    }

    private static class EPLOtherSingleInsertInto implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String text = "@name('insert') @public insert into SomeEvent select *, myString||myString as concat from SupportBeanSimple#length(5)";
            env.compileDeploy(text, path).addListener("insert");

            String textTwo = "@name('s0') select * from SomeEvent#length(5)";
            env.compileDeploy(textTwo, path).addListener("s0");
            assertSimple(env);
            assertProperties(env, "insert", Collections.emptyMap());

            env.undeployAll();
        }
    }

    private static class EPLOtherJoinInsertInto implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String text = "@name('insert') @public insert into SomeJoinEvent select *, myString||myString as concat " +
                "from SupportBeanSimple#length(5) as eventOne, SupportMarketDataBean#length(5) as eventTwo";
            env.compileDeploy(text, path).addListener("insert");

            String textTwo = "@name('s0') select * from SomeJoinEvent#length(5)";
            env.compileDeploy(textTwo, path).addListener("s0");

            assertNoCommonProperties(env);
            assertProperties(env, "insert", Collections.emptyMap());

            env.undeployAll();
        }
    }

    private static class EPLOtherJoinNoCommonProperties implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eventNameOne = SupportBeanSimple.class.getSimpleName();
            String eventNameTwo = SupportMarketDataBean.class.getSimpleName();
            String text = "@name('s0') select *, myString||myString as concat " +
                "from " + eventNameOne + "#length(5) as eventOne, "
                + eventNameTwo + "#length(5) as eventTwo";
            env.compileDeploy(text).addListener("s0");

            assertNoCommonProperties(env);

            env.undeployAll();

            text = "@name('s0') select *, myString||myString as concat " +
                "from " + eventNameOne + "#length(5) as eventOne, " +
                eventNameTwo + "#length(5) as eventTwo " +
                "where eventOne.myString = eventTwo.symbol";
            env.compileDeploy(text).addListener("s0");

            assertNoCommonProperties(env);

            env.undeployAll();
        }
    }

    private static class EPLOtherJoinCommonProperties implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String eventNameOne = SupportBean_A.class.getSimpleName();
            String eventNameTwo = SupportBean_B.class.getSimpleName();
            String text = "@name('s0') select *, eventOne.id||eventTwo.id as concat " +
                "from " + eventNameOne + "#length(5) as eventOne, " +
                eventNameTwo + "#length(5) as eventTwo ";
            env.compileDeploy(text).addListener("s0");

            assertCommonProperties(env);

            env.undeployAll();

            text = "@name('s0') select *, eventOne.id||eventTwo.id as concat " +
                "from " + eventNameOne + "#length(5) as eventOne, " +
                eventNameTwo + "#length(5) as eventTwo " +
                "where eventOne.id = eventTwo.id";
            env.compileDeploy(text).addListener("s0");

            assertCommonProperties(env);

            env.undeployAll();
        }
    }

    private static class EPLOtherCombinedProperties implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select *, indexed[0].mapped('0ma').value||indexed[0].mapped('0mb').value as concat from SupportBeanCombinedProps#length(5)";
            env.compileDeploy(text).addListener("s0");
            assertCombinedProps(env);
            env.undeployAll();
        }
    }

    private static class EPLOtherWildcardMapEvent implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "@name('s0') select *, theString||theString as concat from MyMapEventIntString#length(5)";
            env.compileDeploy(text).addListener("s0");

            // The map to send into the eventService
            Map<String, Object> props = new HashMap<>();
            props.put("int", 1);
            props.put("theString", "xx");
            env.sendEventMap(props, "MyMapEventIntString");

            // The map of expected results
            Map<String, Object> properties = new HashMap<>();
            properties.put("int", 1);
            properties.put("theString", "xx");
            properties.put("concat", "xxxx");

            assertProperties(env, "s0", properties);

            env.undeployAll();
        }
    }

    private static class EPLOtherInvalidRepeatedProperties implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String text = "select *, myString||myString as myString from SupportBeanSimple#length(5)";
            env.tryInvalidCompile(text, "skip");
        }
    }

    private static void assertNoCommonProperties(RegressionEnvironment env) {
        SupportBeanSimple eventSimple = sendSimpleEvent(env, "string");
        SupportMarketDataBean eventMarket = sendMarketEvent(env, "string");

        env.assertListener("s0", listener -> {
            EventBean theEvent = listener.getLastNewData()[0];
            Map<String, Object> properties = new HashMap<>();
            properties.put("concat", "stringstring");
            assertProperties(env, "s0", properties);
            assertSame(eventSimple, theEvent.get("eventOne"));
            assertSame(eventMarket, theEvent.get("eventTwo"));
        });
    }

    private static void assertSimple(RegressionEnvironment env) {
        SupportBeanSimple theEvent = sendSimpleEvent(env, "string");

        env.assertListener("s0", listener -> {
            assertEquals("stringstring", listener.getLastNewData()[0].get("concat"));
            Map<String, Object> properties = new HashMap<>();
            properties.put("concat", "stringstring");
            properties.put("myString", "string");
            properties.put("myInt", 0);
            assertProperties(env, "s0", properties);

            assertEquals(Pair.class, listener.getLastNewData()[0].getEventType().getUnderlyingType());
            assertTrue(listener.getLastNewData()[0].getUnderlying() instanceof Pair);
            Pair pair = (Pair) listener.getLastNewData()[0].getUnderlying();
            assertEquals(theEvent, pair.getFirst());
            assertEquals("stringstring", ((Map) pair.getSecond()).get("concat"));
        });
    }

    private static void assertCommonProperties(RegressionEnvironment env) {
        sendABEvents(env, "string");
        env.assertListener("s0", listener -> {
            EventBean theEvent = listener.getLastNewData()[0];
            Map<String, Object> properties = new HashMap<>();
            properties.put("concat", "stringstring");
            assertProperties(env, "s0", properties);
            assertNotNull(theEvent.get("eventOne"));
            assertNotNull(theEvent.get("eventTwo"));
        });
    }

    private static void assertCombinedProps(RegressionEnvironment env) {
        sendCombinedProps(env);
        env.assertListener("s0", listener -> {
            EventBean eventBean = listener.getLastNewData()[0];

            assertEquals("0ma0", eventBean.get("indexed[0].mapped('0ma').value"));
            assertEquals("0ma1", eventBean.get("indexed[0].mapped('0mb').value"));
            assertEquals("1ma0", eventBean.get("indexed[1].mapped('1ma').value"));
            assertEquals("1ma1", eventBean.get("indexed[1].mapped('1mb').value"));

            assertEquals("0ma0", eventBean.get("array[0].mapped('0ma').value"));
            assertEquals("1ma1", eventBean.get("array[1].mapped('1mb').value"));

            assertEquals("0ma00ma1", eventBean.get("concat"));
        });
    }

    private static void assertProperties(RegressionEnvironment env, String statementName, Map<String, Object> properties) {
        env.assertListener(statementName, listener -> {
            EventBean theEvent = listener.getLastNewData()[0];
            for (String property : properties.keySet()) {
                assertEquals(properties.get(property), theEvent.get(property));
            }
        });
    }

    private static SupportBeanSimple sendSimpleEvent(RegressionEnvironment env, String s) {
        SupportBeanSimple bean = new SupportBeanSimple(s, 0);
        env.sendEventBean(bean);
        return bean;
    }

    private static SupportMarketDataBean sendMarketEvent(RegressionEnvironment env, String symbol) {
        SupportMarketDataBean bean = new SupportMarketDataBean(symbol, 0.0, 0L, null);
        env.sendEventBean(bean);
        return bean;
    }

    private static void sendABEvents(RegressionEnvironment env, String id) {
        SupportBean_A beanOne = new SupportBean_A(id);
        SupportBean_B beanTwo = new SupportBean_B(id);
        env.sendEventBean(beanOne);
        env.sendEventBean(beanTwo);
    }

    private static void sendCombinedProps(RegressionEnvironment env) {
        env.sendEventBean(SupportBeanCombinedProps.makeDefaultBean());
    }
}
