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
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeApplicationType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.SupportXPathFunctionResolver;
import com.espertech.esper.regressionlib.support.util.SupportXPathVariableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.espertech.esper.regressionlib.support.util.SupportXML.sendXMLEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EventXMLNoSchemaSimpleXMLXPathProperties {
    protected static final String XML_NOSCHEMAEVENT =
        "<myevent>\n" +
            "  <element1>VAL1</element1>\n" +
            "  <element2>\n" +
            "    <element21 id=\"e21_1\">VAL21-1</element21>\n" +
            "    <element21 id=\"e21_2\">VAL21-2</element21>\n" +
            "  </element2>\n" +
            "  <element3 attrString=\"VAL3\" attrNum=\"5\" attrBool=\"true\"/>\n" +
            "  <element4><element41>VAL4-1</element41></element4>\n" +
            "</myevent>";

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLNoSchemaSimpleXMLXPathPropertiesPreconfig());
        execs.add(new EventXMLNoSchemaSimpleXMLXPathPropertiesCreateSchema());
        return execs;
    }

    public static class EventXMLNoSchemaSimpleXMLXPathPropertiesPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            runAssertion(env, "TestXMLNoSchemaTypeWMoreXPath", new RegressionPath());
        }
    }

    public static class EventXMLNoSchemaSimpleXMLXPathPropertiesCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='myevent'," +
                "  xpathFunctionResolver='" + SupportXPathFunctionResolver.class.getName() + "'," +
                "  xpathVariableResolver='" + SupportXPathVariableResolver.class.getName() + "')" +
                "@XMLSchemaField(name='xpathElement1', xpath='/myevent/element1', type='STRING')" +
                "@XMLSchemaField(name='xpathCountE21', xpath='count(/myevent/element2/element21)', type='NUMBER')" +
                "@XMLSchemaField(name='xpathAttrString', xpath='/myevent/element3/@attrString', type='STRING')" +
                "@XMLSchemaField(name='xpathAttrNum', xpath='/myevent/element3/@attrNum', type='NUMBER')" +
                "@XMLSchemaField(name='xpathAttrBool', xpath='/myevent/element3/@attrBool', type='BOOLEAN')" +
                "@XMLSchemaField(name='stringCastLong', xpath='/myevent/element3/@attrNum', type='STRING', castToType='long')" +
                "@XMLSchemaField(name='stringCastDouble', xpath='/myevent/element3/@attrNum', type='STRING', castToType='double')" +
                "@XMLSchemaField(name='numCastInt', xpath='/myevent/element3/@attrNum', type='NUMBER', castToType='int')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            runAssertion(env, "MyEventCreateSchema", path);
        }
    }

    private static void runAssertion(RegressionEnvironment env, String eventTypeName, RegressionPath path) {
        // assert type metadata
        EventType type = env.runtime().getEventTypeService().getEventTypePreconfigured(eventTypeName);
        assertEquals(EventTypeApplicationType.XML, type.getMetadata().getApplicationType());

        EPAssertionUtil.assertEqualsAnyOrder(new Object[]{
            new EventPropertyDescriptor("xpathElement1", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("xpathCountE21", Double.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("xpathAttrString", String.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("xpathAttrNum", Double.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("xpathAttrBool", Boolean.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("stringCastLong", Long.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("stringCastDouble", Double.class, null, false, false, false, false, false),
            new EventPropertyDescriptor("numCastInt", Integer.class, null, false, false, false, false, false),
        }, type.getPropertyDescriptors());

        String stmt = "@name('s0') select xpathElement1, xpathCountE21, xpathAttrString, xpathAttrNum, xpathAttrBool," +
            "stringCastLong," +
            "stringCastDouble," +
            "numCastInt " +
            "from " + eventTypeName + "#length(100)";
        env.compileDeploy(stmt, path).addListener("s0");

        // Generate document with the specified in element1 to confirm we have independent events
        sendEvent(env, "EventA", eventTypeName);
        assertDataSimpleXPath(env, "EventA");

        sendEvent(env, "EventB", eventTypeName);
        assertDataSimpleXPath(env, "EventB");

        env.undeployAll();
    }

    protected static void assertDataSimpleXPath(RegressionEnvironment env, String element1) {
        assertNotNull(env.listener("s0").getLastNewData());
        EventBean theEvent = env.listener("s0").getLastNewData()[0];

        assertEquals(element1, theEvent.get("xpathElement1"));
        assertEquals(2.0, theEvent.get("xpathCountE21"));
        assertEquals("VAL3", theEvent.get("xpathAttrString"));
        assertEquals(5d, theEvent.get("xpathAttrNum"));
        assertEquals(true, theEvent.get("xpathAttrBool"));
        assertEquals(5L, theEvent.get("stringCastLong"));
        assertEquals(5d, theEvent.get("stringCastDouble"));
        assertEquals(5, theEvent.get("numCastInt"));
    }

    public static void sendEvent(RegressionEnvironment env, String value, String typeName) {
        String xml = XML_NOSCHEMAEVENT.replaceAll("VAL1", value);
        log.debug(".sendEvent value=" + value);
        sendXMLEvent(env, xml, typeName);
    }

    private static final Logger log = LoggerFactory.getLogger(EventXMLNoSchemaSimpleXMLXPathProperties.class);
}
