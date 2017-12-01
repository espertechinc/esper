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
package com.espertech.esper.supportregression.event;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.support.SupportEventTypeAssertionUtil;
import org.apache.avro.generic.GenericData;
import org.junit.Assert;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class SupportEventInfra {
    public static final String MAP_TYPENAME = "MyMapEvent";
    public static final String OA_TYPENAME = "MyObjectArrayEvent";
    public static final String XML_TYPENAME = "MyXMLEvent";
    public static final String AVRO_TYPENAME = "MyAvroEvent";

    public static void assertValuesMayConvert(EventBean eventBean, String[] propertyNames, ValueWithExistsFlag[] expected, Function<Object, Object> optionalValueConversion) {
        SupportEventTypeAssertionUtil.assertConsistency(eventBean);
        Object[] receivedValues = new Object[propertyNames.length];
        Object[] expectedValues = new Object[propertyNames.length];
        for (int i = 0; i < receivedValues.length; i++) {
            Object value = eventBean.get(propertyNames[i]);
            if (optionalValueConversion != null) {
                value = optionalValueConversion.apply(value);
            }
            receivedValues[i] = value;
            expectedValues[i] = expected[i].getValue();
        }
        EPAssertionUtil.assertEqualsExactOrder(expectedValues, receivedValues);

        for (int i = 0; i < receivedValues.length; i++) {
            boolean exists = (Boolean) eventBean.get("exists_" + propertyNames[i]);
            Assert.assertEquals("Assertion failed for property 'exists_" + propertyNames[i] + "'", expected[i].isExists(), exists);
        }
    }

    public static void assertValueMayConvert(EventBean eventBean, String propertyName, ValueWithExistsFlag expected, Function<Object, Object> optionalValueConversion) {
        SupportEventTypeAssertionUtil.assertConsistency(eventBean);
        Object value = eventBean.get(propertyName);
        if (optionalValueConversion != null) {
            value = optionalValueConversion.apply(value);
        }
        Assert.assertEquals(expected.getValue(), value);
        Assert.assertEquals(expected.isExists(), eventBean.get("exists_" + propertyName));
    }

    public static LinkedHashMap<String, Object> twoEntryMap(String keyOne, Object valueOne, String keyTwo, Object valueTwo) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put(keyOne, valueOne);
        map.put(keyTwo, valueTwo);
        return map;
    }

    @FunctionalInterface
    public static interface FunctionSendEvent {
        public void apply(EPServiceProvider epService, Object value);
    }

    public static final FunctionSendEvent FMAP = (epService, event) -> {
        epService.getEPRuntime().sendEvent((Map) event, MAP_TYPENAME);
    };

    public static final FunctionSendEvent FOA = (epService, event) -> {
        epService.getEPRuntime().sendEvent((Object[]) event, OA_TYPENAME);
    };

    public static final FunctionSendEvent FBEAN = (epService, event) -> {
        epService.getEPRuntime().sendEvent(event);
    };

    public static final FunctionSendEvent FAVRO = (epService, event) -> {
        GenericData.Record record = (GenericData.Record) event;
        GenericData.get().validate(record.getSchema(), record);
        epService.getEPRuntime().sendEventAvro(event, AVRO_TYPENAME);
    };

    @FunctionalInterface
    public static interface FunctionSendEventWType {
        public void apply(EPServiceProvider epService, Object value, String typeName);
    }

    public static final FunctionSendEventWType FMAPWTYPE = (epService, event, typeName) -> {
        epService.getEPRuntime().sendEvent((Map) event, typeName);
    };

    public static final FunctionSendEventWType FOAWTYPE = (epService, event, typeName) -> {
        epService.getEPRuntime().sendEvent((Object[]) event, typeName);
    };

    public static final FunctionSendEventWType FBEANWTYPE = (epService, event, typeName) -> {
        epService.getEPRuntime().sendEvent(event);
    };

    public static final FunctionSendEventWType FAVROWTYPE = (epService, event, typeName) -> {
        GenericData.Record record = (GenericData.Record) event;
        GenericData.get().validate(record.getSchema(), record);
        epService.getEPRuntime().sendEventAvro(event, typeName);
    };

    public static final FunctionSendEvent FXML = (epService, event) -> {
        String xml;
        if (event.toString().contains("<myevent")) {
            xml = event.toString();
        } else {
            xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<myevent>\n" +
                    "  " + event + "\n" +
                    "</myevent>\n";
        }
        try {
            SupportXML.sendEvent(epService.getEPRuntime(), xml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    public static Function<Object, Object> xmlToValue = (in) -> {
        if (in == null) return null;
        if (in instanceof Attr) {
            return ((Attr) in).getValue();
        }
        if (in instanceof Node) {
            return ((Node) in).getTextContent();
        }
        return "unknown xml value";
    };

}
