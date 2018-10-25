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
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.*;
import com.espertech.esper.regressionlib.support.epl.SupportStaticMethodLib;
import junit.framework.TestCase;
import org.junit.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertNotNull;

public class EPLOtherStreamExpr {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherChainedParameterized());
        execs.add(new EPLOtherStreamFunction());
        execs.add(new EPLOtherInstanceMethodOuterJoin());
        execs.add(new EPLOtherInstanceMethodStatic());
        execs.add(new EPLOtherStreamInstanceMethodAliased());
        execs.add(new EPLOtherStreamInstanceMethodNoAlias());
        execs.add(new EPLOtherJoinStreamSelectNoWildcard());
        execs.add(new EPLOtherPatternStreamSelectNoWildcard());
        execs.add(new EPLOtherInvalidSelect());
        return execs;
    }

    private static class EPLOtherChainedParameterized implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            String subexpr = "top.getChildOne(\"abc\",10).getChildTwo(\"append\")";
            String epl = "@name('s0') select " + subexpr + " from SupportChainTop as top";
            env.compileDeploy(epl).addListener("s0");
            tryAssertionChainedParam(env, subexpr);
            env.undeployAll();

            env.eplToModelCompileDeploy(epl).addListener("s0");
            tryAssertionChainedParam(env, subexpr);
            env.undeployAll();

            // test property hosts a method
            env.compileDeploy("@name('s0') select inside.getMyString() as val," +
                "inside.insideTwo.getMyOtherString() as val2 " +
                "from SupportBeanStaticOuter").addListener("s0");

            env.sendEventBean(new SupportBeanStaticOuter());
            EventBean result = env.listener("s0").assertOneGetNewAndReset();
            Assert.assertEquals("hello", result.get("val"));
            Assert.assertEquals("hello2", result.get("val2"));
            env.undeployAll();
        }

        private static void tryAssertionChainedParam(RegressionEnvironment env, String subexpr) {

            Object[][] rows = new Object[][]{
                {subexpr, SupportChainChildTwo.class}
            };
            for (int i = 0; i < rows.length; i++) {
                EventPropertyDescriptor prop = env.statement("s0").getEventType().getPropertyDescriptors()[i];
                Assert.assertEquals(rows[i][0], prop.getPropertyName());
                Assert.assertEquals(rows[i][1], prop.getPropertyType());
            }

            env.sendEventBean(new SupportChainTop());
            Object result = env.listener("s0").assertOneGetNewAndReset().get(subexpr);
            Assert.assertEquals("abcappend", ((SupportChainChildTwo) result).getText());
        }
    }

    private static class EPLOtherStreamFunction implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String prefix = "@name('s0') select * from SupportMarketDataBean as s0 where " +
                SupportStaticMethodLib.class.getName();
            tryAssertionStreamFunction(env, prefix + ".volumeGreaterZero(s0)");
            tryAssertionStreamFunction(env, prefix + ".volumeGreaterZero(*)");
            tryAssertionStreamFunction(env, prefix + ".volumeGreaterZeroEventBean(s0)");
            tryAssertionStreamFunction(env, prefix + ".volumeGreaterZeroEventBean(*)");
        }

        private static void tryAssertionStreamFunction(RegressionEnvironment env, String epl) {

            env.compileDeploy(epl).addListener("s0");

            env.sendEventBean(new SupportMarketDataBean("ACME", 0, 0L, null));
            TestCase.assertFalse(env.listener("s0").isInvoked());
            env.sendEventBean(new SupportMarketDataBean("ACME", 0, 100L, null));
            TestCase.assertTrue(env.listener("s0").isInvoked());

            env.undeployAll();
        }
    }

    private static class EPLOtherInstanceMethodOuterJoin implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String textOne = "@name('s0') select symbol, s1.getTheString() as theString from " +
                "SupportMarketDataBean#keepall as s0 " +
                "left outer join " +
                "SupportBean#keepall as s1 on s0.symbol=s1.theString";
            env.compileDeploy(textOne).addListener("s0");

            SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 0, 0L, null);
            env.sendEventBean(eventA);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"symbol", "theString"}, new Object[]{"ACME", null});

            env.undeployAll();
        }
    }

    private static class EPLOtherInstanceMethodStatic implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String textOne = "@name('s0') select symbol, s1.getSimpleProperty() as simpleprop, s1.makeDefaultBean() as def from " +
                "SupportMarketDataBean#keepall as s0 " +
                "left outer join " +
                "SupportBeanComplexProps#keepall as s1 on s0.symbol=s1.simpleProperty";
            env.compileDeploy(textOne).addListener("s0");

            SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 0, 0L, null);
            env.sendEventBean(eventA);
            EventBean theEvent = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(theEvent, new String[]{"symbol", "simpleprop"}, new Object[]{"ACME", null});
            TestCase.assertNull(theEvent.get("def"));

            SupportBeanComplexProps eventComplexProps = SupportBeanComplexProps.makeDefaultBean();
            eventComplexProps.setSimpleProperty("ACME");
            env.sendEventBean(eventComplexProps);
            theEvent = env.listener("s0").assertOneGetNewAndReset();
            EPAssertionUtil.assertProps(theEvent, new String[]{"symbol", "simpleprop"}, new Object[]{"ACME", "ACME"});
            assertNotNull(theEvent.get("def"));

            env.undeployAll();
        }
    }

    private static class EPLOtherStreamInstanceMethodAliased implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String textOne = "@name('s0') select s0.getVolume() as volume, s0.getSymbol() as symbol, s0.getPriceTimesVolume(2) as pvf from " +
                "SupportMarketDataBean as s0 ";
            env.compileDeploy(textOne).addListener("s0");

            EventType type = env.statement("s0").getEventType();
            Assert.assertEquals(3, type.getPropertyNames().length);
            Assert.assertEquals(Long.class, type.getPropertyType("volume"));
            Assert.assertEquals(String.class, type.getPropertyType("symbol"));
            Assert.assertEquals(Double.class, type.getPropertyType("pvf"));

            SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 4, 99L, null);
            env.sendEventBean(eventA);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"volume", "symbol", "pvf"}, new Object[]{99L, "ACME", 4d * 99L * 2});

            env.undeployAll();
        }
    }

    private static class EPLOtherStreamInstanceMethodNoAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String textOne = "@name('s0') select s0.getVolume(), s0.getPriceTimesVolume(3) from " +
                "SupportMarketDataBean as s0 ";
            env.compileDeploy(textOne).addListener("s0");

            EventType type = env.statement("s0").getEventType();
            Assert.assertEquals(2, type.getPropertyNames().length);
            Assert.assertEquals(Long.class, type.getPropertyType("s0.getVolume()"));
            Assert.assertEquals(Double.class, type.getPropertyType("s0.getPriceTimesVolume(3)"));

            SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 4, 2L, null);
            env.sendEventBean(eventA);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"s0.getVolume()", "s0.getPriceTimesVolume(3)"}, new Object[]{2L, 4d * 2L * 3d});
            env.undeployAll();

            // try instance method that accepts EventBean
            String epl = "create schema MyTestEvent as " + MyTestEvent.class.getName() + ";\n" +
                "@name('s0') select " +
                "s0.getValueAsInt(s0, 'id') as c0," +
                "s0.getValueAsInt(*, 'id') as c1" +
                " from MyTestEvent as s0";
            env.compileDeployWBusPublicType(epl, new RegressionPath()).addListener("s0");

            env.sendEventBean(new MyTestEvent(10));
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0,c1".split(","), new Object[]{10, 10});

            env.undeployAll();
        }
    }

    private static class EPLOtherJoinStreamSelectNoWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // try with alias
            String textOne = "@name('s0') select s0 as s0stream, s1 as s1stream from " +
                "SupportMarketDataBean#keepall as s0, " +
                "SupportBean#keepall as s1";

            // Attach listener to feed
            env.compileDeploy(textOne).addListener("s0");
            EPStatementObjectModel model = env.eplToModel(textOne);
            Assert.assertEquals(textOne, model.toEPL());

            EventType type = env.statement("s0").getEventType();
            Assert.assertEquals(2, type.getPropertyNames().length);
            Assert.assertEquals(SupportMarketDataBean.class, type.getPropertyType("s0stream"));
            Assert.assertEquals(SupportBean.class, type.getPropertyType("s1stream"));

            SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 0, 0L, null);
            env.sendEventBean(eventA);

            SupportBean eventB = new SupportBean();
            env.sendEventBean(eventB);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"s0stream", "s1stream"}, new Object[]{eventA, eventB});

            env.undeployAll();

            // try no alias
            textOne = "@name('s0') select s0, s1 from " +
                "SupportMarketDataBean#keepall as s0, " +
                "SupportBean#keepall as s1";
            env.compileDeploy(textOne).addListener("s0");

            type = env.statement("s0").getEventType();
            Assert.assertEquals(2, type.getPropertyNames().length);
            Assert.assertEquals(SupportMarketDataBean.class, type.getPropertyType("s0"));
            Assert.assertEquals(SupportBean.class, type.getPropertyType("s1"));

            env.sendEventBean(eventA);
            env.sendEventBean(eventB);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"s0", "s1"}, new Object[]{eventA, eventB});

            env.undeployAll();
        }
    }

    private static class EPLOtherPatternStreamSelectNoWildcard implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // try with alias
            String textOne = "@name('s0') select * from pattern [every e1=SupportMarketDataBean -> e2=" +
                "SupportBean(" + SupportStaticMethodLib.class.getName() + ".compareEvents(e1, e2))]";
            env.compileDeploy(textOne).addListener("s0");

            SupportMarketDataBean eventA = new SupportMarketDataBean("ACME", 0, 0L, null);
            env.sendEventBean(eventA);

            SupportBean eventB = new SupportBean("ACME", 1);
            env.sendEventBean(eventB);
            EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), new String[]{"e1", "e2"}, new Object[]{eventA, eventB});

            env.undeployAll();
        }
    }

    private static class EPLOtherInvalidSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            tryInvalidCompile(env, "select s0.getString(1,2,3) from SupportBean as s0",
                "skip");

            tryInvalidCompile(env, "select s0.abc() from SupportBean as s0",
                "Failed to validate select-clause expression 's0.abc()': Failed to solve 'abc' to either an date-time or enumeration method, an event property or a method on the event underlying object: Failed to resolve method 'abc': Could not find enumeration method, date-time method or instance method named 'abc' in class '" + SupportBean.class.getName() + "' taking no parameters [");

            tryInvalidCompile(env, "select s.theString from pattern [every [2] s=SupportBean] ee",
                "Failed to validate select-clause expression 's.theString': Failed to resolve property 's.theString' (property 's' is an indexed property and requires an index or enumeration method to access values)");
        }
    }

    public static class MyTestEvent implements Serializable {

        private int id;

        private MyTestEvent(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public int getValueAsInt(EventBean event, String propertyName) {
            return (Integer) event.get(propertyName);
        }
    }
}
