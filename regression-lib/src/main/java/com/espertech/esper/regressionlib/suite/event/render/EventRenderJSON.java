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

import com.espertech.esper.common.client.render.JSONEventRenderer;
import com.espertech.esper.common.internal.event.render.OutputValueRendererJSONString;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class EventRenderJSON {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventRenderRenderSimple());
        execs.add(new EventRenderMapAndNestedArray());
        execs.add(new EventRenderEmptyMap());
        execs.add(new EventRenderEnquote());
        return execs;
    }

    private static class EventRenderRenderSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportBean bean = new SupportBean();
            bean.setTheString("a\nc>");
            bean.setIntPrimitive(1);
            bean.setIntBoxed(992);
            bean.setCharPrimitive('x');
            bean.setEnumValue(SupportEnum.ENUM_VALUE_1);

            env.compileDeploy("@name('s0') select * from SupportBean").addListener("s0");
            env.sendEventBean(bean);

            String result = env.runtime().getRenderEventService().renderJSON("supportBean", env.statement("s0").iterator().next());

            //System.out.println(result);
            String valuesOnly = "{ \"bigDecimal\": null, \"bigInteger\": null, \"boolBoxed\": null, \"boolPrimitive\": false, \"byteBoxed\": null, \"bytePrimitive\": 0, \"charBoxed\": null, \"charPrimitive\": \"x\", \"doubleBoxed\": null, \"doublePrimitive\": 0.0, \"enumValue\": \"ENUM_VALUE_1\", \"floatBoxed\": null, \"floatPrimitive\": 0.0, \"intBoxed\": 992, \"intPrimitive\": 1, \"longBoxed\": null, \"longPrimitive\": 0, \"shortBoxed\": null, \"shortPrimitive\": 0, \"theString\": \"a\\nc>\" }";
            String expected = "{ \"supportBean\": " + valuesOnly + " }";
            assertEquals(removeNewline(expected), removeNewline(result));

            JSONEventRenderer renderer = env.runtime().getRenderEventService().getJSONRenderer(env.statement("s0").getEventType());
            String jsonEvent = renderer.render("supportBean", env.statement("s0").iterator().next());
            assertEquals(removeNewline(expected), removeNewline(jsonEvent));

            jsonEvent = renderer.render(env.statement("s0").iterator().next());
            assertEquals(removeNewline(valuesOnly), removeNewline(jsonEvent));

            env.undeployAll();
        }
    }

    private static class EventRenderMapAndNestedArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from OuterMap").addListener("s0");

            Map<String, Object> dataInner = new LinkedHashMap<String, Object>();
            dataInner.put("stringarr", new String[]{"a", "b"});
            dataInner.put("prop1", "");
            Map<String, Object> dataInnerTwo = new LinkedHashMap<String, Object>();
            dataInnerTwo.put("stringarr", new String[0]);
            dataInnerTwo.put("prop1", "abcdef");
            Map<String, Object> dataOuter = new LinkedHashMap<String, Object>();
            dataOuter.put("intarr", new int[]{1, 2});
            dataOuter.put("innersimple", dataInner);
            dataOuter.put("innerarray", new Map[]{dataInner, dataInnerTwo});
            dataOuter.put("prop0", new SupportBean_A("A1"));
            env.sendEventMap(dataOuter, "OuterMap");

            String result = env.runtime().getRenderEventService().renderJSON("outerMap", env.iterator("s0").next());

            //System.out.println(result);
            String expected = "{\n" +
                "  \"outerMap\": {\n" +
                "    \"intarr\": [1, 2],\n" +
                "    \"innersimple\": {\n" +
                "      \"prop1\": \"\",\n" +
                "      \"stringarr\": [\"a\", \"b\"]\n" +
                "    },\n" +
                "    \"innerarray\": [{\n" +
                "        \"prop1\": \"\",\n" +
                "        \"stringarr\": [\"a\", \"b\"]\n" +
                "      },\n" +
                "      {\n" +
                "        \"prop1\": \"abcdef\",\n" +
                "        \"stringarr\": []\n" +
                "      }],\n" +
                "    \"prop0\": {\n" +
                "      \"id\": \"A1\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
            assertEquals(removeNewline(expected), removeNewline(result));

            env.undeployAll();
        }
    }

    private static class EventRenderEmptyMap implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from EmptyMapEvent");

            env.sendEventBean(new EmptyMapEvent(null));
            String result = env.runtime().getRenderEventService().renderJSON("outer", env.iterator("s0").next());
            String expected = "{ \"outer\": { \"props\": null } }";
            assertEquals(removeNewline(expected), removeNewline(result));

            env.sendEventBean(new EmptyMapEvent(Collections.<String, String>emptyMap()));
            result = env.runtime().getRenderEventService().renderJSON("outer", env.iterator("s0").next());
            expected = "{ \"outer\": { \"props\": {} } }";
            assertEquals(removeNewline(expected), removeNewline(result));

            env.sendEventBean(new EmptyMapEvent(Collections.singletonMap("a", "b")));
            result = env.runtime().getRenderEventService().renderJSON("outer", env.iterator("s0").next());
            expected = "{ \"outer\": { \"props\": { \"a\": \"b\" } } }";
            assertEquals(removeNewline(expected), removeNewline(result));

            env.undeployAll();
        }
    }

    private static class EventRenderEnquote implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[][] testdata = new String[][]{
                {"\t", "\"\\t\""},
                {"\n", "\"\\n\""},
                {"\r", "\"\\r\""},
                {Character.toString((char) 0), "\"\\u0000\""},
            };

            for (int i = 0; i < testdata.length; i++) {
                StringBuilder buf = new StringBuilder();
                OutputValueRendererJSONString.enquote(testdata[i][0], buf);
                assertEquals(testdata[i][1], buf.toString());
            }
        }
    }

    private static String removeNewline(String text) {
        return text.replaceAll("\\s\\s+|\\n|\\r", " ").trim();
    }

    public static class EmptyMapEvent {
        private Map<String, String> props;

        public EmptyMapEvent(Map<String, String> props) {
            this.props = props;
        }

        public Map<String, String> getProps() {
            return props;
        }
    }
}
