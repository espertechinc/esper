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
package com.espertech.esper.regressionlib.suite.event.xml;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyDescriptor;
import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportXML;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EventXMLSchemaEventTransposePrimitiveArray {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLSchemaEventTransposePrimitiveArrayPreconfig());
        execs.add(new EventXMLSchemaEventTransposePrimitiveArrayCreateSchema());
        return execs;
    }

    public static class EventXMLSchemaEventTransposePrimitiveArrayPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "TestNested2", "ABCType", new RegressionPath());
        }
    }

    public static class EventXMLSchemaEventTransposePrimitiveArrayCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String schemaUriSimpleSchema = Thread.currentThread().getContextClassLoader().getResource("regression/simpleSchema.xsd").toString();
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='//nested2', schemaResource='" + schemaUriSimpleSchema + "', eventSenderValidatesRoot=false)" +
                "create xml schema MyEventCreateSchemaNested();\n" +
                "" +
                "@public @buseventtype " +
                "@XMLSchema(rootElementName='simpleEvent', schemaResource='" + schemaUriSimpleSchema + "', eventSenderValidatesRoot=false)" +
                "create xml schema MyEventCreateSchemaABC();\n";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchemaNested", "MyEventCreateSchemaABC", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeNameNested, String eventTypeNameABC, RegressionPath path) {
        // try array property in select
        env.compileDeploy("@name('s0') select * from " + eventTypeNameNested + "#lastevent", path).addListener("s0");

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("prop3", Integer[].class, null, false, false, true, false, false),
        }, env.statement("s0").getEventType().getPropertyDescriptors());
        SupportEventTypeAssertionUtil.assertConsistency(env.statement("s0").getEventType());

        EventSender sender = env.eventService().getEventSender(eventTypeNameNested);
        sender.sendEvent(SupportXML.getDocument("<nested2><prop3>2</prop3><prop3></prop3><prop3>4</prop3></nested2>"));
        EventBean theEvent = env.iterator("s0").next();
        EPAssertionUtil.assertEqualsExactOrder((Integer[]) theEvent.get("prop3"), new Object[]{2, null, 4});
        SupportEventTypeAssertionUtil.assertConsistency(theEvent);
        env.undeployModuleContaining("s0");

        // try array property nested
        env.compileDeploy("@name('s0') select nested3.* from " + eventTypeNameABC + "#lastevent", path);
        SupportXML.sendDefaultEvent(env.eventService(), "test", eventTypeNameABC);
        EventBean stmtSelectResult = env.iterator("s0").next();
        SupportEventTypeAssertionUtil.assertConsistency(stmtSelectResult);
        assertEquals(String[].class, stmtSelectResult.getEventType().getPropertyType("nested4[2].prop5"));
        assertEquals("SAMPLE_V8", stmtSelectResult.get("nested4[0].prop5[1]"));
        EPAssertionUtil.assertEqualsExactOrder((String[]) stmtSelectResult.get("nested4[2].prop5"), new Object[]{"SAMPLE_V10", "SAMPLE_V11"});

        EventBean fragmentNested4 = (EventBean) stmtSelectResult.getFragment("nested4[2]");
        EPAssertionUtil.assertEqualsExactOrder((String[]) fragmentNested4.get("prop5"), new Object[]{"SAMPLE_V10", "SAMPLE_V11"});
        assertEquals("SAMPLE_V11", fragmentNested4.get("prop5[1]"));
        SupportEventTypeAssertionUtil.assertConsistency(fragmentNested4);

        env.undeployAll();
    }
}
