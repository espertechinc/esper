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
package com.espertech.esper.regression.event.bean;

import com.espertech.esper.client.*;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanDupProperty;
import com.espertech.esper.supportregression.bean.SupportBeanReservedKeyword;
import com.espertech.esper.supportregression.bean.SupportBeanWriteOnly;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;

public class ExecEventBeanPropertyResolutionWDefaults implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionReservedKeywordEscape(epService);
        runAssertionWriteOnly(epService);
        runAssertionCaseSensitive(epService);
    }

    private void runAssertionReservedKeywordEscape(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType("SomeKeywords", SupportBeanReservedKeyword.class);
        epService.getEPAdministrator().getConfiguration().addEventType("Order", SupportBeanReservedKeyword.class);
        SupportUpdateListener listener = new SupportUpdateListener();

        EPStatement stmt = epService.getEPAdministrator().createEPL("select `seconds`, `order` from SomeKeywords");
        stmt.addListener(listener);

        Object theEvent = new SupportBeanReservedKeyword(1, 2);
        epService.getEPRuntime().sendEvent(theEvent);
        EventBean eventBean = listener.assertOneGetNewAndReset();
        assertEquals(1, eventBean.get("seconds"));
        assertEquals(2, eventBean.get("order"));

        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select * from `Order`");
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(theEvent);
        eventBean = listener.assertOneGetNewAndReset();
        assertEquals(1, eventBean.get("seconds"));
        assertEquals(2, eventBean.get("order"));

        stmt.destroy();
        stmt = epService.getEPAdministrator().createEPL("select timestamp.`hour` as val from SomeKeywords");
        stmt.addListener(listener);

        SupportBeanReservedKeyword bean = new SupportBeanReservedKeyword(1, 2);
        bean.setTimestamp(new SupportBeanReservedKeyword.Inner());
        bean.getTimestamp().setHour(10);
        epService.getEPRuntime().sendEvent(bean);
        eventBean = listener.assertOneGetNewAndReset();
        assertEquals(10, eventBean.get("val"));

        // test back-tick with spaces etc
        Map<String, Object> defType = new HashMap<String, Object>();
        defType.put("candidate book", String.class);
        defType.put("XML Message Type", String.class);
        defType.put("select", int.class);
        defType.put("children's books", int[].class);
        defType.put("my <> map", Map.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyType", defType);
        epService.getEPAdministrator().createEPL("select `candidate book` as c0, `XML Message Type` as c1, `select` as c2, `children's books`[0] as c3, `my <> map`('xx') as c4 from MyType").addListener(listener);

        Map<String, Object> defValues = new HashMap<String, Object>();
        defValues.put("candidate book", "Enders Game");
        defValues.put("XML Message Type", "book");
        defValues.put("select", 100);
        defValues.put("children's books", new int[]{50, 51});
        defValues.put("my <> map", Collections.singletonMap("xx", "abc"));
        epService.getEPRuntime().sendEvent(defValues, "MyType");
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0,c1,c2,c3,c4".split(","), new Object[]{"Enders Game", "book", 100, 50, "abc"});

        try {
            epService.getEPAdministrator().createEPL("select `select` from " + SupportBean.class.getName());
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Failed to validate select-clause expression 'select': Property named 'select' is not valid in any stream [");
        }

        try {
            epService.getEPAdministrator().createEPL("select `ab cd` from " + SupportBean.class.getName());
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Error starting statement: Failed to validate select-clause expression 'ab cd': Property named 'ab cd' is not valid in any stream [");
        }

        // test resolution as nested property
        epService.getEPAdministrator().createEPL("create schema MyEvent as (customer string, `from` string)");
        epService.getEPAdministrator().createEPL("insert into DerivedStream select customer,`from` from MyEvent");
        epService.getEPAdministrator().createEPL("create window TheWindow#firstunique(customer,`from`) as DerivedStream");
        epService.getEPAdministrator().createEPL("on pattern [a=TheWindow -> timer:interval(12 hours)] as s0 delete from TheWindow as s1 where s0.a.`from`=s1.`from`");

        // test escape in column name
        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL("select theString as `order`, theString as `price.for.goods` from SupportBean");
        stmtTwo.addListener(listener);
        assertEquals(String.class, stmtTwo.getEventType().getPropertyType("order"));
        assertEquals("price.for.goods", stmtTwo.getEventType().getPropertyDescriptors()[1].getPropertyName());

        epService.getEPRuntime().sendEvent(new SupportBean("E1", 1));
        Map<String, Object> out = (Map<String, Object>) listener.assertOneGetNew().getUnderlying();
        assertEquals("E1", out.get("order"));
        assertEquals("E1", out.get("price.for.goods"));

        // try control character
        tryInvalidControlCharacter(listener.assertOneGetNew());

        // try enum with keyword
        tryEnumWithKeyword(epService);

        tryEnumItselfReserved(epService);

        epService.getEPAdministrator().destroyAllStatements();
    }

    private void runAssertionWriteOnly(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select * from " + SupportBeanWriteOnly.class.getName());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        Object theEvent = new SupportBeanWriteOnly();
        epService.getEPRuntime().sendEvent(theEvent);
        EventBean eventBean = listener.assertOneGetNewAndReset();
        assertSame(theEvent, eventBean.getUnderlying());

        EventType type = stmt.getEventType();
        assertEquals(0, type.getPropertyNames().length);

        stmt.destroy();
    }

    private void runAssertionCaseSensitive(EPServiceProvider epService) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select MYPROPERTY, myproperty, myProperty from " + SupportBeanDupProperty.class.getName());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBeanDupProperty("lowercamel", "uppercamel", "upper", "lower"));
        EventBean result = listener.assertOneGetNewAndReset();
        assertEquals("upper", result.get("MYPROPERTY"));
        assertEquals("lower", result.get("myproperty"));
        assertTrue(result.get("myProperty").equals("lowercamel") || result.get("myProperty").equals("uppercamel")); // JDK6 versus JDK7 JavaBean inspector

        stmt.destroy();
        try {
            epService.getEPAdministrator().createEPL("select MyProperty from " + SupportBeanDupProperty.class.getName());
            fail();
        } catch (EPException ex) {
            // expected
        }
    }

    private void tryEnumWithKeyword(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(LocalEventWithEnum.class);
        epService.getEPAdministrator().getConfiguration().addImport(LocalEventEnum.class);
        epService.getEPAdministrator().createEPL("select * from LocalEventWithEnum(localEventEnum=LocalEventEnum.`NEW`)");
    }

    private void tryInvalidControlCharacter(EventBean eventBean) {
        try {
            eventBean.get("a\u008F");
            fail();
        } catch (PropertyAccessException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "Property named 'a\u008F' is not a valid property name for this type");
        }
    }

    private void tryEnumItselfReserved(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addEventType(LocalEventWithGroup.class);
        epService.getEPAdministrator().getConfiguration().addImport(GROUP.class);
        epService.getEPAdministrator().createEPL("select * from LocalEventWithGroup(`GROUP`=`GROUP`.FOO)");
    }

    public static class LocalEventWithEnum {
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

    public static class LocalEventWithGroup {
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
