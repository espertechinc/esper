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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.client.soda.*;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionFlag;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.bean.SupportBeanComplexProps;
import com.espertech.esper.regressionlib.support.bean.SupportMarketDataBean;

import java.util.*;

import static org.junit.Assert.*;

public class EPLOtherSelectExprStreamSelector {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLOtherInvalidSelectWildcardProperty());
        execs.add(new EPLOtherInsertTransposeNestedProperty());
        execs.add(new EPLOtherInsertFromPattern());
        execs.add(new EPLOtherObjectModelJoinAlias());
        execs.add(new EPLOtherNoJoinWildcardNoAlias());
        execs.add(new EPLOtherJoinWildcardNoAlias());
        execs.add(new EPLOtherNoJoinWildcardWithAlias());
        execs.add(new EPLOtherJoinWildcardWithAlias());
        execs.add(new EPLOtherNoJoinWithAliasWithProperties());
        execs.add(new EPLOtherJoinWithAliasWithProperties());
        execs.add(new EPLOtherNoJoinNoAliasWithProperties());
        execs.add(new EPLOtherJoinNoAliasWithProperties());
        execs.add(new EPLOtherAloneNoJoinNoAlias());
        execs.add(new EPLOtherAloneNoJoinAlias());
        execs.add(new EPLOtherAloneJoinAlias());
        execs.add(new EPLOtherAloneJoinNoAlias());
        execs.add(new EPLOtherInvalidSelect());
        return execs;
    }

    private static class EPLOtherInvalidSelectWildcardProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.tryInvalidCompile("select simpleProperty.* as a from SupportBeanComplexProps as s0",
                "The property wildcard syntax must be used without column name");
        }
    }

    private static class EPLOtherInsertTransposeNestedProperty implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String stmtOneText = "@name('l1') @public insert into StreamA select nested.* from SupportBeanComplexProps as s0";
            env.compileDeploy(stmtOneText, path).addListener("l1");
            env.assertStatement("l1", statement -> assertEquals(SupportBeanComplexProps.SupportBeanSpecialGetterNested.class, statement.getEventType().getUnderlyingType()));

            String stmtTwoText = "@name('l2') select nestedValue from StreamA";
            env.compileDeploy(stmtTwoText, path).addListener("l2");
            env.assertStatement("l2", statement -> assertEquals(String.class, statement.getEventType().getPropertyType("nestedValue")));

            env.sendEventBean(SupportBeanComplexProps.makeDefaultBean());

            env.assertEqualsNew("l1", "nestedValue", "nestedValue");
            env.assertEqualsNew("l2", "nestedValue", "nestedValue");

            env.undeployAll();
            env.undeployAll();
        }
    }

    private static class EPLOtherInsertFromPattern implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String stmtOneText = "@name('l1') insert into streamA select a.* from pattern [every a=SupportBean]";
            env.compileDeploy(stmtOneText).addListener("l1");

            String stmtTwoText = "@name('l2') insert into streamA select a.* from pattern [every a=SupportBean where timer:within(30 sec)]";
            env.compileDeploy(stmtTwoText).addListener("l2");

            env.assertStatement("l1", statement -> {
                EventType eventType = statement.getEventType();
                assertEquals(SupportBean.class, eventType.getUnderlyingType());
            });

            Object theEventOne = sendBeanEvent(env, "E1", 10);
            env.assertEventNew("l2", event -> assertSame(theEventOne, event.getUnderlying()));

            Object theEventTwo = sendBeanEvent(env, "E2", 10);
            env.assertEventNew("l2", event -> assertSame(theEventTwo, event.getUnderlying()));

            String stmtThreeText = "@name('l3') insert into streamB select a.*, 'abc' as abc from pattern [every a=SupportBean where timer:within(30 sec)]";
            env.compileDeploy(stmtThreeText);
            env.assertStatement("l3", statement -> {
                assertEquals(Pair.class, statement.getEventType().getUnderlyingType());
                assertEquals(String.class, statement.getEventType().getPropertyType("abc"));
                assertEquals(String.class, statement.getEventType().getPropertyType("theString"));
            });

            env.undeployAll();
        }

        public EnumSet<RegressionFlag> flags() {
            return EnumSet.of(RegressionFlag.VISIBILITY);
        }
    }

    private static class EPLOtherObjectModelJoinAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EPStatementObjectModel model = new EPStatementObjectModel();
            model.setSelectClause(SelectClause.create()
                .addStreamWildcard("s0")
                .addStreamWildcard("s1", "s1stream")
                .addWithAsProvidedName("theString", "sym"));
            model.setFromClause(FromClause.create()
                .add(FilterStream.create("SupportBean", "s0").addView("keepall"))
                .add(FilterStream.create("SupportMarketDataBean", "s1").addView("keepall")));
            model.setAnnotations(Collections.singletonList(AnnotationPart.nameAnnotation("s0")));
            env.compileDeploy(model).addListener("s0");

            String epl = "@name('s0') select s0.*, s1.* as s1stream, theString as sym from SupportBean#keepall as s0, " +
                "SupportMarketDataBean#keepall as s1";
            assertEquals(epl, model.toEPL());
            EPStatementObjectModel modelReverse = env.eplToModel(model.toEPL());
            assertEquals(epl, modelReverse.toEPL());

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(SupportMarketDataBean.class, type.getPropertyType("s1stream"));
                assertEquals(Pair.class, type.getUnderlyingType());
            });

            sendBeanEvent(env, "E1");
            env.assertListenerNotInvoked("s0");

            Object theEvent = sendMarketEvent(env, "E1");
            env.assertEventNew("s0", event -> assertSame(theEvent, event.get("s1stream")));

            env.undeployAll();
        }
    }

    private static class EPLOtherNoJoinWildcardNoAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select *, win.* from SupportBean#length(3) as win";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertTrue(type.getPropertyNames().length > 15);
                assertEquals(SupportBean.class, type.getUnderlyingType());
            });

            Object theEvent = sendBeanEvent(env, "E1", 16);
            env.assertEventNew("s0", event -> assertSame(theEvent, event.getUnderlying()));

            env.undeployAll();
        }
    }

    private static class EPLOtherJoinWildcardNoAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select *, s1.* from SupportBean#length(3) as s0, " +
                "SupportMarketDataBean#keepall as s1";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(7, type.getPropertyNames().length);
                assertEquals(Long.class, type.getPropertyType("volume"));
                assertEquals(SupportBean.class, type.getPropertyType("s0"));
                assertEquals(SupportMarketDataBean.class, type.getPropertyType("s1"));
                assertEquals(Pair.class, type.getUnderlyingType());
            });

            Object eventOne = sendBeanEvent(env, "E1", 13);
            env.assertListenerNotInvoked("s0");

            Object eventTwo = sendMarketEvent(env, "E2");
            String[] fields = new String[]{"s0", "s1", "symbol", "volume"};
            env.assertPropsNew("s0", fields, new Object[]{eventOne, eventTwo, "E2", 0L});

            env.undeployAll();
        }
    }

    private static class EPLOtherNoJoinWildcardWithAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select *, win.* as s0 from SupportBean#length(3) as win";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertTrue(type.getPropertyNames().length > 15);
                assertEquals(Pair.class, type.getUnderlyingType());
                assertEquals(SupportBean.class, type.getPropertyType("s0"));
            });

            Object theEvent = sendBeanEvent(env, "E1", 15);
            String[] fields = new String[]{"theString", "intPrimitive", "s0"};
            env.assertPropsNew("s0", fields, new Object[]{"E1", 15, theEvent});

            env.undeployAll();
        }
    }

    private static class EPLOtherJoinWildcardWithAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select *, s1.* as s1stream, s0.* as s0stream from SupportBean#length(3) as s0, " +
                "SupportMarketDataBean#keepall as s1";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(4, type.getPropertyNames().length);
                assertEquals(SupportBean.class, type.getPropertyType("s0stream"));
                assertEquals(SupportBean.class, type.getPropertyType("s0"));
                assertEquals(SupportMarketDataBean.class, type.getPropertyType("s1stream"));
                assertEquals(SupportMarketDataBean.class, type.getPropertyType("s1"));
                assertEquals(Map.class, type.getUnderlyingType());
            });

            Object eventOne = sendBeanEvent(env, "E1", 13);
            env.assertListenerNotInvoked("s0");

            Object eventTwo = sendMarketEvent(env, "E2");
            String[] fields = new String[]{"s0", "s1", "s0stream", "s1stream"};
            env.assertPropsNew("s0", fields, new Object[]{eventOne, eventTwo, eventOne, eventTwo});

            env.undeployAll();
        }
    }

    private static class EPLOtherNoJoinWithAliasWithProperties implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select theString.* as s0, intPrimitive as a, theString.* as s1, intPrimitive as b from SupportBean#length(3) as theString";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(4, type.getPropertyNames().length);
                assertEquals(Map.class, type.getUnderlyingType());
                assertEquals(Integer.class, type.getPropertyType("a"));
                assertEquals(Integer.class, type.getPropertyType("b"));
                assertEquals(SupportBean.class, type.getPropertyType("s0"));
                assertEquals(SupportBean.class, type.getPropertyType("s1"));
            });

            Object theEvent = sendBeanEvent(env, "E1", 12);
            String[] fields = new String[]{"s0", "s1", "a", "b"};
            env.assertPropsNew("s0", fields, new Object[]{theEvent, theEvent, 12, 12});

            env.undeployAll();
        }
    }

    private static class EPLOtherJoinWithAliasWithProperties implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intPrimitive, s1.* as s1stream, theString, symbol as sym, s0.* as s0stream from SupportBean#length(3) as s0, " +
                "SupportMarketDataBean#keepall as s1";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(5, type.getPropertyNames().length);
                assertEquals(Integer.class, type.getPropertyType("intPrimitive"));
                assertEquals(SupportMarketDataBean.class, type.getPropertyType("s1stream"));
                assertEquals(SupportBean.class, type.getPropertyType("s0stream"));
                assertEquals(String.class, type.getPropertyType("sym"));
                assertEquals(String.class, type.getPropertyType("theString"));
                assertEquals(Map.class, type.getUnderlyingType());
            });

            Object eventOne = sendBeanEvent(env, "E1", 13);
            env.assertListenerNotInvoked("s0");

            Object eventTwo = sendMarketEvent(env, "E2");
            String[] fields = new String[]{"intPrimitive", "sym", "theString", "s0stream", "s1stream"};
            env.assertEventNew("s0", event -> {
                EPAssertionUtil.assertProps(event, fields, new Object[]{13, "E2", "E1", eventOne, eventTwo});
                EventBean theEvent = (EventBean) ((Map) event.getUnderlying()).get("s0stream");
                assertSame(eventOne, theEvent.getUnderlying());
            });

            env.undeployAll();
        }
    }

    private static class EPLOtherNoJoinNoAliasWithProperties implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intPrimitive as a, string.*, intPrimitive as b from SupportBean#length(3) as string";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(22, type.getPropertyNames().length);
                assertEquals(Pair.class, type.getUnderlyingType());
                assertEquals(Integer.class, type.getPropertyType("a"));
                assertEquals(Integer.class, type.getPropertyType("b"));
                assertEquals(String.class, type.getPropertyType("theString"));
            });

            sendBeanEvent(env, "E1", 10);
            String[] fields = new String[]{"a", "theString", "intPrimitive", "b"};
            env.assertPropsNew("s0", fields, new Object[]{10, "E1", 10, 10});

            env.undeployAll();
        }
    }

    private static class EPLOtherJoinNoAliasWithProperties implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select intPrimitive, s1.*, symbol as sym from SupportBean#length(3) as s0, " +
                "SupportMarketDataBean#keepall as s1";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(7, type.getPropertyNames().length);
                assertEquals(Integer.class, type.getPropertyType("intPrimitive"));
                assertEquals(Pair.class, type.getUnderlyingType());
            });

            sendBeanEvent(env, "E1", 11);
            env.assertListenerNotInvoked("s0");

            Object theEvent = sendMarketEvent(env, "E1");
            String[] fields = new String[]{"intPrimitive", "sym", "symbol"};
            env.assertEventNew("s0", event -> {
                EPAssertionUtil.assertProps(event, fields, new Object[]{11, "E1", "E1"});
                assertSame(theEvent, ((Pair) event.getUnderlying()).getFirst());
            });

            env.undeployAll();
        }
    }

    private static class EPLOtherAloneNoJoinNoAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select theString.* from SupportBean#length(3) as theString";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertTrue(type.getPropertyNames().length > 10);
                assertEquals(SupportBean.class, type.getUnderlyingType());
            });

            Object theEvent = sendBeanEvent(env, "E1");
            env.assertEventNew("s0", event -> assertSame(theEvent, event.getUnderlying()));

            env.undeployAll();
        }
    }

    private static class EPLOtherAloneNoJoinAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select theString.* as s0 from SupportBean#length(3) as theString";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(1, type.getPropertyNames().length);
                assertEquals(SupportBean.class, type.getPropertyType("s0"));
                assertEquals(Map.class, type.getUnderlyingType());
            });

            Object theEvent = sendBeanEvent(env, "E1");
            env.assertEventNew("s0", event -> assertSame(theEvent, event.get("s0")));

            env.undeployAll();
        }
    }

    private static class EPLOtherAloneJoinAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select s1.* as s1 from SupportBean#length(3) as s0, " +
                "SupportMarketDataBean#keepall as s1";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(SupportMarketDataBean.class, type.getPropertyType("s1"));
                assertEquals(Map.class, type.getUnderlyingType());
            });

            sendBeanEvent(env, "E1");
            env.assertListenerNotInvoked("s0");

            Object eventOne = sendMarketEvent(env, "E1");
            env.assertEventNew("s0", event -> assertSame(eventOne, event.get("s1")));

            env.undeployAll();

            // reverse streams
            epl = "@name('s0') select s0.* as szero from SupportBean#length(3) as s0, " +
                "SupportMarketDataBean#keepall as s1";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(SupportBean.class, type.getPropertyType("szero"));
                assertEquals(Map.class, type.getUnderlyingType());
            });

            sendMarketEvent(env, "E1");
            env.assertListenerNotInvoked("s0");

            Object eventTwo = sendBeanEvent(env, "E1");
            env.assertEventNew("s0", event -> assertSame(eventTwo, event.get("szero")));

            env.undeployAll();
        }
    }

    private static class EPLOtherAloneJoinNoAlias implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@name('s0') select s1.* from SupportBean#length(3) as s0, " +
                "SupportMarketDataBean#keepall as s1";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(Long.class, type.getPropertyType("volume"));
                assertEquals(SupportMarketDataBean.class, type.getUnderlyingType());
            });

            sendBeanEvent(env, "E1");
            env.assertListenerNotInvoked("s0");

            Object eventOne = sendMarketEvent(env, "E1");
            env.assertEventNew("s0", event -> assertSame(eventOne, event.getUnderlying()));

            env.undeployAll();

            // reverse streams
            epl = "@name('s0') select s0.* from SupportBean#length(3) as s0, " +
                "SupportMarketDataBean#keepall as s1";
            env.compileDeploy(epl).addListener("s0");

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(String.class, type.getPropertyType("theString"));
                assertEquals(SupportBean.class, type.getUnderlyingType());
            });

            sendMarketEvent(env, "E1");
            env.assertListenerNotInvoked("s0");

            Object eventTwo = sendBeanEvent(env, "E1");
            env.assertEventNew("s0", event -> assertSame(eventTwo, event.getUnderlying()));

            env.undeployAll();
        }
    }

    private static class EPLOtherInvalidSelect implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.tryInvalidCompile("select theString.* as theString, theString from SupportBean#length(3) as theString",
                "Column name 'theString' appears more then once in select clause");

            env.tryInvalidCompile("select s1.* as abc from SupportBean#length(3) as s0",
                "Stream selector 's1.*' does not match any stream name in the from clause [");

            env.tryInvalidCompile("select s0.* as abc, s0.* as abc from SupportBean#length(3) as s0",
                "Column name 'abc' appears more then once in select clause");

            env.tryInvalidCompile("select s0.*, s1.* from SupportBean#keepall as s0, SupportBean#keepall as s1",
                "A column name must be supplied for all but one stream if multiple streams are selected via the stream.* notation");
        }
    }

    private static SupportBean sendBeanEvent(RegressionEnvironment env, String s) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        env.sendEventBean(bean);
        return bean;
    }

    private static SupportBean sendBeanEvent(RegressionEnvironment env, String s, int intPrimitive) {
        SupportBean bean = new SupportBean();
        bean.setTheString(s);
        bean.setIntPrimitive(intPrimitive);
        env.sendEventBean(bean);
        return bean;
    }

    private static SupportMarketDataBean sendMarketEvent(RegressionEnvironment env, String s) {
        SupportMarketDataBean bean = new SupportMarketDataBean(s, 0d, 0L, "");
        env.sendEventBean(bean);
        return bean;
    }
}
