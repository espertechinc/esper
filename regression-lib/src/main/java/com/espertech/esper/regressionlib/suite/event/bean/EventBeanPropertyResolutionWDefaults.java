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
package com.espertech.esper.regressionlib.suite.event.bean;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.regressionlib.support.bean.SupportBeanDupProperty;
import com.espertech.esper.regressionlib.support.bean.SupportBeanReservedKeyword;
import com.espertech.esper.regressionlib.support.bean.SupportBeanWriteOnly;

import java.io.Serializable;
import java.util.*;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class EventBeanPropertyResolutionWDefaults {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLBeanReservedKeywordEscape());
        execs.add(new EPLBeanWriteOnly());
        execs.add(new EPLBeanCaseSensitive());
        return execs;
    }

    private static class EPLBeanReservedKeywordEscape implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select `seconds`, `order` from SomeKeywords").addListener("s0");

            Object theEvent = new SupportBeanReservedKeyword(1, 2);
            env.sendEventBean(theEvent, "SomeKeywords");
            env.assertEventNew("s0", eventBean -> {
                assertEquals(1, eventBean.get("seconds"));
                assertEquals(2, eventBean.get("order"));
            });

            env.undeployAll();
            env.compileDeploy("@name('s0') select * from `Order`").addListener("s0");

            env.sendEventBean(theEvent, "Order");
            env.assertEventNew("s0", eventBean -> {
                assertEquals(1, eventBean.get("seconds"));
                assertEquals(2, eventBean.get("order"));
            });

            env.undeployAll();
            env.compileDeploy("@name('s0') select timestamp.`hour` as val from SomeKeywords").addListener("s0");

            SupportBeanReservedKeyword bean = new SupportBeanReservedKeyword(1, 2);
            bean.setTimestamp(new SupportBeanReservedKeyword.Inner());
            bean.getTimestamp().setHour(10);
            env.sendEventBean(bean, "SomeKeywords");
            env.assertEqualsNew("s0", "val", 10);
            env.undeployAll();

            // test back-tick with spaces etc
            env.compileDeploy("@name('s0') select `candidate book` as c0, `XML Message Type` as c1, `select` as c2, `children's books`[0] as c3, `my <> map`('xx') as c4 from MyType").addListener("s0");

            Map<String, Object> defValues = new HashMap<String, Object>();
            defValues.put("candidate book", "Enders Game");
            defValues.put("XML Message Type", "book");
            defValues.put("select", 100);
            defValues.put("children's books", new int[]{50, 51});
            defValues.put("my <> map", Collections.singletonMap("xx", "abc"));
            env.sendEventMap(defValues, "MyType");
            env.assertPropsNew("s0", "c0,c1,c2,c3,c4".split(","), new Object[]{"Enders Game", "book", 100, 50, "abc"});
            env.undeployAll();

            env.tryInvalidCompile("select `select` from SupportBean", "Failed to validate select-clause expression 'select': Property named '`select`' is not valid in any stream [");
            env.tryInvalidCompile("select `ab cd` from SupportBean", "Failed to validate select-clause expression 'ab cd': Property named '`ab cd`' is not valid in any stream [");

            // test resolution as nested property
            RegressionPath path = new RegressionPath();
            env.compileDeploy("@public create schema MyEvent as (customer string, `from` string)", path);
            env.compileDeploy("@public insert into DerivedStream select customer,`from` from MyEvent", path);
            env.compileDeploy("@public create window TheWindow#firstunique(customer,`from`) as DerivedStream", path);
            env.compileDeploy("on pattern [a=TheWindow -> timer:interval(12 hours)] as s0 delete from TheWindow as s1 where s0.a.`from`=s1.`from`", path);

            // test escape in column name
            env.compileDeploy("@name('s0') select theString as `order`, theString as `price.for.goods` from SupportBean").addListener("s0");
            env.assertStatement("s0", statement -> {
                assertEquals(String.class, statement.getEventType().getPropertyType("order"));
                assertEquals("price.for.goods", statement.getEventType().getPropertyDescriptors()[1].getPropertyName());
            });

            env.sendEventBean(new SupportBean("E1", 1));
            env.assertEventNew("s0", eventBean -> {
                Map<String, Object> out = (Map<String, Object>) eventBean.getUnderlying();
                assertEquals("E1", out.get("order"));
                assertEquals("E1", out.get("price.for.goods"));

                // try control character
                tryInvalidControlCharacter(eventBean);
            });

            // try enum with keyword
            tryEnumWithKeyword(env);

            tryEnumItselfReserved(env);

            env.undeployAll();
        }
    }

    private static class EPLBeanWriteOnly implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from SupportBeanWriteOnly").addListener("s0");

            Object theEvent = new SupportBeanWriteOnly();
            env.sendEventBean(theEvent);
            env.assertEventNew("s0", eventBean -> assertSame(theEvent, eventBean.getUnderlying()));

            env.assertStatement("s0", statement -> {
                EventType type = statement.getEventType();
                assertEquals(0, type.getPropertyNames().length);
            });

            env.undeployAll();
        }
    }

    private static class EPLBeanCaseSensitive implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select MYPROPERTY, myproperty, myProperty from SupportBeanDupProperty").addListener("s0");

            env.sendEventBean(new SupportBeanDupProperty("lowercamel", "uppercamel", "upper", "lower"));
            env.assertEventNew("s0", result -> {
                assertEquals("upper", result.get("MYPROPERTY"));
                assertEquals("lower", result.get("myproperty"));
                assertTrue(result.get("myProperty").equals("lowercamel") || result.get("myProperty").equals("uppercamel")); // JDK6 versus JDK7 JavaBean inspector
            });

            env.undeployAll();
            env.tryInvalidCompile("select MyProperty from SupportBeanDupProperty",
                "Failed to validate select-clause expression 'MyProperty': Property named 'MyProperty' is not valid in any stream (did you mean 'MYPROPERTY'?)");
        }
    }

    private static void tryEnumWithKeyword(RegressionEnvironment env) {
        env.compileDeploy("select * from LocalEventWithEnum(localEventEnum=LocalEventEnum.`NEW`)");
    }

    private static void tryInvalidControlCharacter(EventBean eventBean) {
        try {
            eventBean.get("a\u008F");
            fail();
        } catch (PropertyAccessException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Property named 'a\u008F' is not a valid property name for this type");
        }
    }

    private static void tryEnumItselfReserved(RegressionEnvironment env) {
        env.compileDeploy("select * from LocalEventWithGroup(`GROUP`=`GROUP`.FOO)");
    }

    public static class LocalEventWithEnum implements Serializable {
        private LocalEventEnum localEventEnum;

        public LocalEventWithEnum(LocalEventEnum localEventEnum) {
            this.localEventEnum = localEventEnum;
        }

        public LocalEventEnum getLocalEventEnum() {
            return localEventEnum;
        }
    }

    public static enum LocalEventEnum {
        NEW;
    }

    public static class LocalEventWithGroup implements Serializable {
        private GROUP group;

        public LocalEventWithGroup(GROUP group) {
            this.group = group;
        }

        public GROUP getGROUP() {
            return group;
        }
    }

    public static enum GROUP {
        FOO, BAR;
    }
}
