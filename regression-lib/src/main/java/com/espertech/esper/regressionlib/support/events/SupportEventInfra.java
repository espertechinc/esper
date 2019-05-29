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
package com.espertech.esper.regressionlib.support.events;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.support.SupportEventTypeAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.support.util.SupportXML;
import org.apache.avro.generic.GenericData;
import org.junit.Assert;
import org.w3c.dom.Attr;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.function.Function;

public class SupportEventInfra {

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

    @FunctionalInterface
    public static interface FunctionSendEvent {
        public void apply(RegressionEnvironment env, Object value, String name);
    }

    public static final FunctionSendEvent FMAP = (env, event, name) -> {
        env.sendEventMap((Map) event, name);
    };

    public static final FunctionSendEvent FOA = (env, event, name) -> {
        env.sendEventObjectArray((Object[]) event, name);
    };

    public static final FunctionSendEvent FBEAN = (env, event, name) -> {
        env.sendEventBean(event, name);
    };

    public static final FunctionSendEvent FAVRO = (env, event, name) -> {
        GenericData.Record record = (GenericData.Record) event;
        GenericData.get().validate(record.getSchema(), record);
        env.sendEventAvro(record, name);
    };

    public static final FunctionSendEvent FJSON = (env, event, name) -> {
        env.sendEventJson((String) event, name);
    };

    @FunctionalInterface
    public static interface FunctionSendEventWType {
        public void apply(RegressionEnvironment env, Object value, String typeName);
    }

    public static final FunctionSendEventWType FMAPWTYPE = (env, event, typeName) -> {
        env.sendEventMap((Map) event, typeName);
    };

    public static final FunctionSendEventWType FOAWTYPE = (env, event, typeName) -> {
        env.sendEventObjectArray((Object[]) event, typeName);
    };

    public static final FunctionSendEventWType FBEANWTYPE = (env, event, typeName) -> {
        env.sendEventBean(event);
    };

    public static final FunctionSendEventWType FAVROWTYPE = (env, event, typeName) -> {
        GenericData.Record record = (GenericData.Record) event;
        GenericData.get().validate(record.getSchema(), record);
        env.sendEventAvro(record, typeName);
    };

    public static final FunctionSendEventWType FJSONWTYPE = (env, event, typeName) -> {
        env.sendEventJson((String) event, typeName);
    };

    public static final FunctionSendEvent FXML = (env, event, name) -> {
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
            SupportXML.sendXMLEvent(env, xml, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    public static Function<Object, Object> xmlToValue = in -> {
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
