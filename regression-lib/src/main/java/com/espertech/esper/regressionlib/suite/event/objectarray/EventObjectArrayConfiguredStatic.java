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
package com.espertech.esper.regressionlib.suite.event.objectarray;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EventObjectArrayConfiguredStatic {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventObjectArrayConfiguredStaticCore());
        execs.add(new EventObjectArrayConfiguredStaticContextWIndex());
        return execs;
    }

    public static class EventObjectArrayConfiguredStaticContextWIndex implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "create schema MyEventType(symbol string, price double, timestamp long);\n" +
                "create context MyContext partition by symbol, price from MyEventType;\n" +
                "context MyContext select a.timestamp as timestamp_a, b.timestamp as timestamp_b\n" +
                "from MyEventType#time(10) as a, MyEventType#time(10) as b\n" +
                "where a.timestamp.coincides(b.timestamp, 10 milliseconds) and a.symbol=b.symbol and a.price=b.price;";
            env.compileDeploy(epl).undeployAll();
        }
    }

    public static class EventObjectArrayConfiguredStaticCore implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.assertThat(() -> {
                EventType eventType = env.runtime().getEventTypeService().getEventTypePreconfigured("MyOAType");
                assertEquals(Object[].class, eventType.getUnderlyingType());
                assertEquals(String.class, eventType.getPropertyType("theString"));
                assertEquals(Map.class, eventType.getPropertyType("map"));
                assertEquals(SupportBean.class, eventType.getPropertyType("bean"));
            });

            env.compileDeploy("@name('s0') select bean, theString, map('key'), bean.theString from MyOAType");
            env.addListener("s0");

            env.assertStatement("s0", statement -> assertEquals(Object[].class, statement.getEventType().getUnderlyingType()));

            SupportBean bean = new SupportBean("E1", 1);
            env.sendEventObjectArray(new Object[]{bean, "abc", Collections.singletonMap("key", "value")}, "MyOAType");
            env.assertPropsNew("s0", "bean,theString,map('key'),bean.theString".split(","), new Object[]{bean, "abc", "value", "E1"});

            env.undeployAll();
        }
    }
}
