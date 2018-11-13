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
package com.espertech.esper.regressionlib.suite.event.render;

import com.espertech.esper.common.client.render.EventPropertyRenderer;
import com.espertech.esper.common.client.render.EventPropertyRendererContext;
import com.espertech.esper.common.client.render.JSONRenderingOptions;
import com.espertech.esper.common.client.render.XMLRenderingOptions;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBeanRendererOne;
import com.espertech.esper.regressionlib.support.bean.SupportBeanRendererThree;
import com.espertech.esper.common.internal.support.SupportBean_S0;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EventRender {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventRenderPropertyCustomRenderer());
        execs.add(new EventRenderObjectArray());
        execs.add(new EventRenderPOJOMap());
        return execs;
    }

    private static class EventRenderPropertyCustomRenderer implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            env.compileDeploy("@name('s0') select * from MyRendererEvent");
            env.sendEventBean(new MyRendererEvent("id1", new Object[][]{{1, "x"}, {2, "y"}}));

            MyRenderer.getContexts().clear();
            JSONRenderingOptions jsonOptions = new JSONRenderingOptions();
            jsonOptions.setRenderer(new MyRenderer());
            String json = env.runtime().getRenderEventService().renderJSON("MyEvent", env.iterator("s0").next(), jsonOptions);
            assertEquals(4, MyRenderer.getContexts().size());
            List<EventPropertyRendererContext> contexts = MyRenderer.getContexts();
            EventPropertyRendererContext context = contexts.get(2);
            assertNotNull(context.getDefaultRenderer());
            assertEquals(1, (int) context.getIndexedPropertyIndex());
            assertEquals(MyRendererEvent.class.getSimpleName(), context.getEventType().getName());
            assertEquals("someProperties", context.getPropertyName());

            String expectedJson = "{ \"MyEvent\": { \"id\": \"id1\", \"someProperties\": [\"index#0=1;index#1=x\", \"index#0=2;index#1=y\"], \"mappedProperty\": { \"key\": \"value\" } } }";
            assertEquals(removeNewline(expectedJson), removeNewline(json));

            MyRenderer.getContexts().clear();
            XMLRenderingOptions xmlOptions = new XMLRenderingOptions();
            xmlOptions.setRenderer(new MyRenderer());
            String xmlOne = env.runtime().getRenderEventService().renderXML("MyEvent", env.iterator("s0").next(), xmlOptions);
            String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <MyEvent> <id>id1</id> <someProperties>index#0=1;index#1=x</someProperties> <someProperties>index#0=2;index#1=y</someProperties> <mappedProperty> <key>value</key> </mappedProperty> </MyEvent>";
            assertEquals(4, MyRenderer.getContexts().size());
            assertEquals(removeNewline(expected), removeNewline(xmlOne));

            env.undeployAll();
        }
    }

    private static class EventRenderObjectArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            Object[] values = {"abc", 1, new SupportBean_S0(1, "p00"), 2L, 3d};
            env.compileDeploy("@name('s0') select * from MyObjectArrayType");
            env.sendEventObjectArray(values, "MyObjectArrayType");

            String json = env.runtime().getRenderEventService().renderJSON("MyEvent", env.iterator("s0").next());
            String expectedJson = "{ \"MyEvent\": { \"p0\": \"abc\", \"p1\": 1, \"p3\": 2, \"p4\": 3.0, \"p2\": { \"id\": 1, \"p00\": \"p00\", \"p01\": null, \"p02\": null, \"p03\": null } } }";
            assertEquals(removeNewline(expectedJson), removeNewline(json));

            String xmlOne = env.runtime().getRenderEventService().renderXML("MyEvent", env.iterator("s0").next());
            String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <MyEvent> <p0>abc</p0> <p1>1</p1> <p3>2</p3> <p4>3.0</p4> <p2> <id>1</id> <p00>p00</p00> </p2> </MyEvent>";
            assertEquals(removeNewline(expected), removeNewline(xmlOne));

            env.undeployAll();
        }
    }

    private static class EventRenderPOJOMap implements RegressionExecution {
        @Override
        public boolean excludeWhenInstrumented() {
            return true;
        }

        public void run(RegressionEnvironment env) {

            SupportBeanRendererOne beanOne = new SupportBeanRendererOne();
            Map<String, Object> otherMap = new LinkedHashMap<String, Object>();
            otherMap.put("abc", "def");
            otherMap.put("def", 123);
            otherMap.put("efg", null);
            otherMap.put(null, 1234);
            beanOne.setStringObjectMap(otherMap);

            env.compileDeploy("@name('s0') select * from SupportBeanRendererOne");
            env.sendEventBean(beanOne);

            String json = env.runtime().getRenderEventService().renderJSON("MyEvent", env.iterator("s0").next());
            String expectedJson = "{ \"MyEvent\": { \"stringObjectMap\": { \"abc\": \"def\", \"def\": 123, \"efg\": null } } }";
            assertEquals(removeNewline(expectedJson), removeNewline(json));

            String xmlOne = env.runtime().getRenderEventService().renderXML("MyEvent", env.iterator("s0").next());
            String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<MyEvent>\n" +
                "  <stringObjectMap>\n" +
                "    <abc>def</abc>\n" +
                "    <def>123</def>\n" +
                "    <efg></efg>\n" +
                "  </stringObjectMap>\n" +
                "</MyEvent>";
            assertEquals(removeNewline(expected), removeNewline(xmlOne));

            XMLRenderingOptions opt = new XMLRenderingOptions();
            opt.setDefaultAsAttribute(true);
            String xmlTwo = env.runtime().getRenderEventService().renderXML("MyEvent", env.iterator("s0").next(), opt);
            String expectedTwo = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<MyEvent>\n" +
                "  <stringObjectMap abc=\"def\" def=\"123\"/>\n" +
                "</MyEvent>";
            assertEquals(removeNewline(expectedTwo), removeNewline(xmlTwo));
            env.undeployModuleContaining("s0");

            // try the same Map only undeclared
            SupportBeanRendererThree beanThree = new SupportBeanRendererThree();
            beanThree.setStringObjectMap(otherMap);
            env.compileDeploy("@name('s0') select * from SupportBeanRendererThree");
            env.sendEventBean(beanThree);
            json = env.runtime().getRenderEventService().renderJSON("MyEvent", env.iterator("s0").next());
            assertEquals(removeNewline(expectedJson), removeNewline(json));

            env.undeployAll();
        }
    }

    private static String removeNewline(String text) {
        return text.replaceAll("\\s\\s+|\\n|\\r", " ").trim();
    }

    public static class MyRendererEvent {
        private final String id;
        private final Object[][] someProperties;

        public MyRendererEvent(String id, Object[][] someProperties) {
            this.id = id;
            this.someProperties = someProperties;
        }

        public String getId() {
            return id;
        }

        public Object[][] getSomeProperties() {
            return someProperties;
        }

        public Map<String, Object> getMappedProperty() {
            return Collections.<String, Object>singletonMap("key", "value");
        }
    }

    public static class MyRenderer implements EventPropertyRenderer {

        private static List<EventPropertyRendererContext> contexts = new ArrayList<EventPropertyRendererContext>();

        public void render(EventPropertyRendererContext context) {
            if (context.getPropertyName().equals("someProperties")) {
                Object[] value = (Object[]) context.getPropertyValue();

                StringBuilder builder = context.getStringBuilder();
                if (context.isJsonFormatted()) {
                    context.getStringBuilder().append("\"");
                }
                String delimiter = "";
                for (int i = 0; i < value.length; i++) {
                    builder.append(delimiter);
                    builder.append("index#");
                    builder.append(Integer.toString(i));
                    builder.append("=");
                    builder.append(value[i]);
                    delimiter = ";";
                }
                if (context.isJsonFormatted()) {
                    context.getStringBuilder().append("\"");
                }
            } else {
                context.getDefaultRenderer().render(context.getPropertyValue(), context.getStringBuilder());
            }

            contexts.add(context.copy());
        }

        public static List<EventPropertyRendererContext> getContexts() {
            return contexts;
        }

        public static void setContexts(List<EventPropertyRendererContext> contexts) {
            MyRenderer.contexts = contexts;
        }
    }
}
