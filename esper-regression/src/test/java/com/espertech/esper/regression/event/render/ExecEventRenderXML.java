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
package com.espertech.esper.regression.event.render;

import com.espertech.esper.client.*;
import com.espertech.esper.client.util.XMLRenderingOptions;
import com.espertech.esper.event.util.OutputValueRendererXMLString;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_A;
import com.espertech.esper.supportregression.bean.SupportEnum;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExecEventRenderXML implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getViewResources().setIterableUnbound(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        runAssertionRenderSimple(epService);
        runAssertionMapAndNestedArray(epService);
        runAssertionSQLDate(epService);
        runAssertionEnquote();
    }

    private void runAssertionRenderSimple(EPServiceProvider epService) {
        SupportBean bean = new SupportBean();
        bean.setTheString("a\nc");
        bean.setIntPrimitive(1);
        bean.setIntBoxed(992);
        bean.setCharPrimitive('x');
        bean.setEnumValue(SupportEnum.ENUM_VALUE_2);

        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        EPStatement statement = epService.getEPAdministrator().createEPL("select * from SupportBean");
        epService.getEPRuntime().sendEvent(bean);

        String result = epService.getEPRuntime().getEventRenderer().renderXML("supportBean", statement.iterator().next());
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
                "  <this>\n" +
                "    <boolPrimitive>false</boolPrimitive>\n" +
                "    <bytePrimitive>0</bytePrimitive>\n" +
                "    <charPrimitive>x</charPrimitive>\n" +
                "    <doublePrimitive>0.0</doublePrimitive>\n" +
                "    <enumValue>ENUM_VALUE_2</enumValue>\n" +
                "    <floatPrimitive>0.0</floatPrimitive>\n" +
                "    <intBoxed>992</intBoxed>\n" +
                "    <intPrimitive>1</intPrimitive>\n" +
                "    <longPrimitive>0</longPrimitive>\n" +
                "    <shortPrimitive>0</shortPrimitive>\n" +
                "    <theString>a\\u000ac</theString>\n" +
                "  </this>\n" +
                "</supportBean>";
        assertEquals(removeNewline(expected), removeNewline(result));

        result = epService.getEPRuntime().getEventRenderer().renderXML("supportBean", statement.iterator().next(), new XMLRenderingOptions().setDefaultAsAttribute(true));
        // System.out.println(result);
        expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <supportBean boolPrimitive=\"false\" bytePrimitive=\"0\" charPrimitive=\"x\" doublePrimitive=\"0.0\" enumValue=\"ENUM_VALUE_2\" floatPrimitive=\"0.0\" intBoxed=\"992\" intPrimitive=\"1\" longPrimitive=\"0\" shortPrimitive=\"0\" theString=\"a\\u000ac\"> <this boolPrimitive=\"false\" bytePrimitive=\"0\" charPrimitive=\"x\" doublePrimitive=\"0.0\" enumValue=\"ENUM_VALUE_2\" floatPrimitive=\"0.0\" intBoxed=\"992\" intPrimitive=\"1\" longPrimitive=\"0\" shortPrimitive=\"0\" theString=\"a\\u000ac\"/> </supportBean>";
        assertEquals(removeNewline(expected), removeNewline(result));

        statement.destroy();
    }

    private void runAssertionMapAndNestedArray(EPServiceProvider epService) {
        Map<String, Object> defOuter = new LinkedHashMap<String, Object>();
        defOuter.put("intarr", int[].class);
        defOuter.put("innersimple", "InnerMap");
        defOuter.put("innerarray", "InnerMap[]");
        defOuter.put("prop0", SupportBean_A.class);

        Map<String, Object> defInner = new LinkedHashMap<String, Object>();
        defInner.put("stringarr", String[].class);
        defInner.put("prop1", String.class);

        epService.getEPAdministrator().getConfiguration().addEventType("InnerMap", defInner);
        epService.getEPAdministrator().getConfiguration().addEventType("OuterMap", defOuter);
        EPStatement statement = epService.getEPAdministrator().createEPL("select * from OuterMap");

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
        epService.getEPRuntime().sendEvent(dataOuter, "OuterMap");

        String result = epService.getEPRuntime().getEventRenderer().renderXML("outerMap", statement.iterator().next());
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

        result = epService.getEPRuntime().getEventRenderer().renderXML("outerMap xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"", statement.iterator().next(), new XMLRenderingOptions().setDefaultAsAttribute(true));
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

        statement.destroy();
    }

    private void runAssertionSQLDate(EPServiceProvider epService) {
        // ESPER-469
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class);
        EPStatement statement = epService.getEPAdministrator().createEPL("select java.sql.Date.valueOf(\"2010-01-31\") as mySqlDate from SupportBean");
        epService.getEPRuntime().sendEvent(new SupportBean());

        EventBean theEvent = statement.iterator().next();
        assertEquals(java.sql.Date.valueOf("2010-01-31"), theEvent.get("mySqlDate"));
        EventPropertyGetter getter = statement.getEventType().getGetter("mySqlDate");
        assertEquals(java.sql.Date.valueOf("2010-01-31"), getter.get(theEvent));

        String result = epService.getEPRuntime().getEventRenderer().renderXML("testsqldate", theEvent);

        // System.out.println(result);
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <testsqldate> <mySqlDate>2010-01-31</mySqlDate> </testsqldate>";
        assertEquals(removeNewline(expected), removeNewline(result));

        statement.destroy();
    }

    private void runAssertionEnquote() {
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

    private String removeNewline(String text) {
        return text.replaceAll("\\s\\s+|\\n|\\r", " ").trim();
    }
}
