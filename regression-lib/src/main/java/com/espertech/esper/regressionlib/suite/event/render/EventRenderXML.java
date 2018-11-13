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

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventPropertyGetter;
import com.espertech.esper.common.client.render.XMLRenderingOptions;
import com.espertech.esper.common.internal.event.render.OutputValueRendererXMLString;
import com.espertech.esper.common.internal.support.SupportEnum;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.bean.SupportBean_A;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EventRenderXML {
    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventRenderRenderSimple());
        execs.add(new EventRenderMapAndNestedArray());
        execs.add(new EventRenderSQLDate());
        execs.add(new EventRenderEnquote());
        return execs;
    }

    private static class EventRenderRenderSimple implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            SupportBean bean = new SupportBean();
            bean.setTheString("a\nc");
            bean.setIntPrimitive(1);
            bean.setIntBoxed(992);
            bean.setCharPrimitive('x');
            bean.setEnumValue(SupportEnum.ENUM_VALUE_2);

            env.compileDeploy("@name('s0') select * from SupportBean");
            env.sendEventBean(bean);

            String result = env.runtime().getRenderEventService().renderXML("supportBean", env.iterator("s0").next());
            //System.out.println(result);
            String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<supportBean>\n" +
                "  <boolPrimitive>false</boolPrimitive>\n" +
                "  <bytePrimitive>0</bytePrimitive>\n" +
                "  <charPrimitive>x</charPrimitive>\n" +
                "  <doublePrimitive>0.0</doublePrimitive>\n" +
                "  <enumValue>ENUM_VALUE_2</enumValue>\n" +
                "  <floatPrimitive>0.0</floatPrimitive>\n" +
                "  <intBoxed>992</intBoxed>\n" +
                "  <intPrimitive>1</intPrimitive>\n" +
                "  <longPrimitive>0</longPrimitive>\n" +
                "  <shortPrimitive>0</shortPrimitive>\n" +
                "  <theString>a\\u000ac</theString>\n" +
                "</supportBean>";
            assertEquals(removeNewline(expected), removeNewline(result));

            result = env.runtime().getRenderEventService().renderXML("supportBean", env.iterator("s0").next(), new XMLRenderingOptions().setDefaultAsAttribute(true));
            // System.out.println(result);
            expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <supportBean boolPrimitive=\"false\" bytePrimitive=\"0\" charPrimitive=\"x\" doublePrimitive=\"0.0\" enumValue=\"ENUM_VALUE_2\" floatPrimitive=\"0.0\" intBoxed=\"992\" intPrimitive=\"1\" longPrimitive=\"0\" shortPrimitive=\"0\" theString=\"a\\u000ac\"/>";
            assertEquals(removeNewline(expected), removeNewline(result));

            env.undeployAll();
        }
    }

    private static class EventRenderMapAndNestedArray implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            env.compileDeploy("@name('s0') select * from OuterMap").addListener("s0");

            Map<String, Object> dataInner = new LinkedHashMap<String, Object>();
            dataInner.put("stringarr", new String[]{"a", null});
            dataInner.put("prop1", "");
            Map<String, Object> dataArrayOne = new LinkedHashMap<String, Object>();
            dataArrayOne.put("stringarr", new String[0]);
            dataArrayOne.put("prop1", "abcdef");
            Map<String, Object> dataArrayTwo = new LinkedHashMap<String, Object>();
            dataArrayTwo.put("stringarr", new String[]{"R&R", "a>b"});
            dataArrayTwo.put("prop1", "");
            Map<String, Object> dataArrayThree = new LinkedHashMap<String, Object>();
            dataArrayOne.put("stringarr", null);
            Map<String, Object> dataOuter = new LinkedHashMap<String, Object>();
            dataOuter.put("intarr", new int[]{1, 2});
            dataOuter.put("innersimple", dataInner);
            dataOuter.put("innerarray", new Map[]{dataArrayOne, dataArrayTwo, dataArrayThree});
            dataOuter.put("prop0", new SupportBean_A("A1"));
            env.sendEventMap(dataOuter, "OuterMap");

            String result = env.runtime().getRenderEventService().renderXML("outerMap", env.iterator("s0").next());
            // System.out.println(result);
            String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<outerMap>\n" +
                "  <intarr>1</intarr>\n" +
                "  <intarr>2</intarr>\n" +
                "  <innersimple>\n" +
                "    <prop1></prop1>\n" +
                "    <stringarr>a</stringarr>\n" +
                "  </innersimple>\n" +
                "  <innerarray>\n" +
                "    <prop1>abcdef</prop1>\n" +
                "  </innerarray>\n" +
                "  <innerarray>\n" +
                "    <prop1></prop1>\n" +
                "    <stringarr>R&amp;R</stringarr>\n" +
                "    <stringarr>a&gt;b</stringarr>\n" +
                "  </innerarray>\n" +
                "  <innerarray>\n" +
                "  </innerarray>\n" +
                "  <prop0>\n" +
                "    <id>A1</id>\n" +
                "  </prop0>\n" +
                "</outerMap>";
            assertEquals(removeNewline(expected), removeNewline(result));

            result = env.runtime().getRenderEventService().renderXML("outerMap xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"", env.iterator("s0").next(), new XMLRenderingOptions().setDefaultAsAttribute(true));
            // System.out.println(result);
            expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<outerMap xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "  <intarr>1</intarr>\n" +
                "  <intarr>2</intarr>\n" +
                "  <innersimple prop1=\"\">\n" +
                "    <stringarr>a</stringarr>\n" +
                "  </innersimple>\n" +
                "  <innerarray prop1=\"abcdef\"/>\n" +
                "  <innerarray prop1=\"\">\n" +
                "    <stringarr>R&amp;R</stringarr>\n" +
                "    <stringarr>a&gt;b</stringarr>\n" +
                "  </innerarray>\n" +
                "  <innerarray/>\n" +
                "  <prop0 id=\"A1\"/>\n" +
                "</outerMap>";
            assertEquals(removeNewline(expected), removeNewline(result));

            env.undeployAll();
        }
    }

    private static class EventRenderSQLDate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // ESPER-469
            env.compileDeploy("@name('s0') select java.sql.Date.valueOf(\"2010-01-31\") as mySqlDate from SupportBean");
            env.sendEventBean(new SupportBean());

            EventBean theEvent = env.iterator("s0").next();
            assertEquals(java.sql.Date.valueOf("2010-01-31"), theEvent.get("mySqlDate"));
            EventPropertyGetter getter = env.statement("s0").getEventType().getGetter("mySqlDate");
            assertEquals(java.sql.Date.valueOf("2010-01-31"), getter.get(theEvent));

            String result = env.runtime().getRenderEventService().renderXML("testsqldate", theEvent);

            // System.out.println(result);
            String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <testsqldate> <mySqlDate>2010-01-31</mySqlDate> </testsqldate>";
            assertEquals(removeNewline(expected), removeNewline(result));

            env.undeployAll();
        }
    }

    private static class EventRenderEnquote implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String[][] testdata = new String[][]{
                {"\"", "&quot;"},
                {"'", "&apos;"},
                {"&", "&amp;"},
                {"<", "&lt;"},
                {">", "&gt;"},
                {Character.toString((char) 0), "\\u0000"},
            };

            for (int i = 0; i < testdata.length; i++) {
                StringBuilder buf = new StringBuilder();
                OutputValueRendererXMLString.xmlEncode(testdata[i][0], buf, true);
                assertEquals(testdata[i][1], buf.toString());
            }
        }
    }

    private static String removeNewline(String text) {
        return text.replaceAll("\\s\\s+|\\n|\\r", " ").trim();
    }
}
