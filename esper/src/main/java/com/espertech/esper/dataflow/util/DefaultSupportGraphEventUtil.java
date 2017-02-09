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
package com.espertech.esper.dataflow.util;

import com.espertech.esper.client.ConfigurationEventTypeXMLDOM;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.event.SendableEvent;
import com.espertech.esper.event.arr.SendableEventObjectArray;
import com.espertech.esper.event.bean.SendableEventBean;
import com.espertech.esper.event.map.SendableEventMap;
import com.espertech.esper.event.xml.SendableEventXML;
import com.espertech.esper.util.FileUtil;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DefaultSupportGraphEventUtil {

    public static final String CLASSLOADER_SCHEMA_URI = "regression/threeProperties.xsd";

    public static void addTypeConfiguration(EPServiceProvider epService) {
        HashMap<String, Object> propertyTypes = new LinkedHashMap<String, Object>();
        propertyTypes.put("myDouble", Double.class);
        propertyTypes.put("myInt", Integer.class);
        propertyTypes.put("myString", String.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyMapEvent", propertyTypes);
        epService.getEPAdministrator().getConfiguration().addEventType("MyOAEvent", "myDouble,myInt,myString".split(","), new Object[]{Double.class, Integer.class, String.class});
        epService.getEPAdministrator().getConfiguration().addEventType(MyEvent.class);
        epService.getEPAdministrator().getConfiguration().addEventType("MyXMLEvent", getConfig());
    }

    public static SendableEvent[] getXMLEventsSendable() {
        Object[] xmlEvents = getXMLEvents();
        SendableEvent[] xmls = new SendableEvent[xmlEvents.length];
        for (int i = 0; i < xmlEvents.length; i++) {
            xmls[i] = new SendableEventXML((Node) xmlEvents[i]);
        }
        return xmls;
    }

    public static SendableEvent[] getOAEventsSendable() {
        Object[] oaEvents = getOAEvents();
        SendableEvent[] oas = new SendableEvent[oaEvents.length];
        for (int i = 0; i < oaEvents.length; i++) {
            oas[i] = new SendableEventObjectArray((Object[]) oaEvents[i], "MyOAEvent");
        }
        return oas;
    }

    public static SendableEvent[] getMapEventsSendable() {
        Object[] mapEvents = getMapEvents();
        SendableEvent[] sendables = new SendableEvent[mapEvents.length];
        for (int i = 0; i < mapEvents.length; i++) {
            sendables[i] = new SendableEventMap((Map<String, Object>) mapEvents[i], "MyMapEvent");
        }
        return sendables;
    }

    public static SendableEvent[] getPOJOEventsSendable() {
        Object[] pojoEvents = getPOJOEvents();
        SendableEvent[] sendables = new SendableEvent[pojoEvents.length];
        for (int i = 0; i < pojoEvents.length; i++) {
            sendables[i] = new SendableEventBean(pojoEvents[i]);
        }
        return sendables;
    }

    public static Object[] getXMLEvents() {
        return new Object[]{makeXMLEvent(1.1d, 1, "one"), makeXMLEvent(2.2d, 2, "two")};
    }

    public static Object[] getOAEvents() {
        return new Object[]{new Object[]{1.1d, 1, "one"}, new Object[]{2.2d, 2, "two"}};
    }

    public static Object[] getMapEvents() {
        return new Object[]{makeMapEvent(1.1, 1, "one"), makeMapEvent(2.2d, 2, "two")};
    }

    public static Object[] getPOJOEvents() {
        return new Object[]{new MyEvent(1.1d, 1, "one"), new MyEvent(2.2d, 2, "two")};
    }

    private static ConfigurationEventTypeXMLDOM getConfig() {
        ConfigurationEventTypeXMLDOM eventTypeMeta = new ConfigurationEventTypeXMLDOM();
        eventTypeMeta.setRootElementName("rootelement");
        InputStream schemaStream = DefaultSupportGraphEventUtil.class.getClassLoader().getResourceAsStream(CLASSLOADER_SCHEMA_URI);
        if (schemaStream == null) {
            throw new IllegalStateException("Failed to load schema '" + CLASSLOADER_SCHEMA_URI + "'");
        }
        String schemaText = FileUtil.linesToText(FileUtil.readFile(schemaStream));
        eventTypeMeta.setSchemaText(schemaText);
        return eventTypeMeta;
    }

    private static Node makeXMLEvent(double myDouble, int myInt, String myString) {
        String xml = "<rootelement myDouble=\"VAL_DBL\" myInt=\"VAL_INT\" myString=\"VAL_STR\" />";
        xml = xml.replaceAll("VAL_DBL", Double.toString(myDouble));
        xml = xml.replaceAll("VAL_INT", Integer.toString(myInt));
        xml = xml.replaceAll("VAL_STR", myString);

        InputSource source = new InputSource(new StringReader(xml));
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        try {
            return builderFactory.newDocumentBuilder().parse(source).getDocumentElement();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse '" + xml + "' as XML: " + e.getMessage(), e);
        }
    }

    private static Map<String, Object> makeMapEvent(double myDouble, int myInt, String myString) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("myDouble", myDouble);
        map.put("myInt", myInt);
        map.put("myString", myString);
        return map;
    }

    public static class MyEvent {
        private final double myDouble;
        private final int myInt;
        private final String myString;

        public MyEvent(double myDouble, int myInt, String myString) {
            this.myDouble = myDouble;
            this.myInt = myInt;
            this.myString = myString;
        }

        public int getMyInt() {
            return myInt;
        }

        public double getMyDouble() {
            return myDouble;
        }

        public String getMyString() {
            return myString;
        }
    }
}
