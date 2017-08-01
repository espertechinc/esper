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
package com.espertech.esper.example.runtimeconfig;

import com.espertech.esper.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Sample for demonstrating runtime configuration operations.
 * <p>
 * Please see the OHLC view for an example of a custom view.
 * Please see the EsperIO Axiom event type for an example of a custom event type and resolution URI.
 */
public class RuntimeConfigMain {
    private static final Logger log = LoggerFactory.getLogger(RuntimeConfigMain.class);

    private EPServiceProvider provider;

    public static void main(String[] args) {
        RuntimeConfigMain main = new RuntimeConfigMain();

        try {
            main.runExample();
        } catch (Exception ex) {
            log.error("Unexpected error occured running example:" + ex.getMessage(), ex);
        }
    }

    public void runExample() {
        // Allocate default provider in default configuration
        provider = EPServiceProviderManager.getDefaultProvider();

        // Add an event type
        Map<String, Object> typeDefinition = new HashMap<String, Object>();
        typeDefinition.put("propertyOne", "string");
        typeDefinition.put("propertyTwo", "int");
        provider.getEPAdministrator().getConfiguration().addEventType("MyEvent", typeDefinition);

        // Configure and test variables
        configureVariables();

        // Configure and test a variant stream
        configureVariantStream();

        // Configure and test an import
        configureImport();

        // Add a single-row function
        configureSingleRowFunction();

        // Add a custom aggregation function
        configureCustomAggregationFunction();

        // Use a revision event type
        configureRevisionType();
    }

    private void configureVariables() {

        provider.getEPAdministrator().getConfiguration().addVariable("myintvar", int.class, 5);

        EPStatement stmt = provider.getEPAdministrator().createEPL("select propertyOne, propertyTwo, myintvar from MyEvent#lastevent");

        // send an event
        Map<String, Object> eventData = new HashMap<String, Object>();
        eventData.put("propertyOne", "value");
        eventData.put("propertyTwo", 10);
        provider.getEPRuntime().sendEvent(eventData, "MyEvent");

        // the statement above keeps the last event for iterating
        EventBean received = stmt.iterator().next();
        System.out.println("\nConfigure Variables:");
        System.out.println("Received:" +
                " propertyOne=" + received.get("propertyOne") +
                " propertyTwo=" + received.get("propertyTwo") +
                " myintvar=" + received.get("myintvar")
        );
    }

    private void configureCustomAggregationFunction() {
        // define a append-string aggregation function provided by another class
        provider.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("concat", MyConcatAggregationFunctionFactory.class.getName());

        // Add an event type that has a string-type property value
        Map<String, Object> typeDefinition = new HashMap<String, Object>();
        typeDefinition.put("str", String.class);
        provider.getEPAdministrator().getConfiguration().addEventType("MyConcatSampleEvent", typeDefinition);

        // keep the last few events from the variant stream
        EPStatement stmt = provider.getEPAdministrator().createEPL("select concat(str) as cstr from MyConcatSampleEvent");

        // send an event
        Map<String, Object> eventData = new HashMap<String, Object>();
        eventData.put("str", "part1");
        provider.getEPRuntime().sendEvent(eventData, "MyConcatSampleEvent");

        // print results
        System.out.println("\nConfigure Aggregation Function:");
        System.out.println("Received:" +
                " cstr=" + stmt.iterator().next().get("cstr"));

        // send a second event
        eventData = new HashMap<String, Object>();
        eventData.put("str", "part2");
        provider.getEPRuntime().sendEvent(eventData, "MyConcatSampleEvent");

        // print results
        System.out.println("Received:" +
                " cstr=" + stmt.iterator().next().get("cstr"));
    }

    private void configureSingleRowFunction() {
        // define a single-row median function provided by this class
        provider.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("mymedian", this.getClass().getName(), "computeDoubleMedian");

        // Add an event type that has a double-array -type property value
        Map<String, Object> typeDefinition = new HashMap<String, Object>();
        typeDefinition.put("doubles", double[].class);
        provider.getEPAdministrator().getConfiguration().addEventType("MyMedianSampleEvent", typeDefinition);

        // keep the last few events from the variant stream
        EPStatement stmt = provider.getEPAdministrator().createEPL("select mymedian(doubles) as med from MyMedianSampleEvent#lastevent");

        // send an event
        Map<String, Object> eventData = new HashMap<String, Object>();
        eventData.put("doubles", new double[]{5, 1, 2, 3, 4});
        provider.getEPRuntime().sendEvent(eventData, "MyMedianSampleEvent");

        // print results
        System.out.println("\nConfigure Single-Row Function:");
        System.out.println("Received:" +
                " med=" + stmt.iterator().next().get("med"));

        // send a second event
        eventData = new HashMap<String, Object>();
        eventData.put("doubles", new double[]{5, 1, 2, 3, 4, 4});
        provider.getEPRuntime().sendEvent(eventData, "MyMedianSampleEvent");

        // print results
        System.out.println("Received:" +
                " med=" + stmt.iterator().next().get("med"));
    }

    private void configureImport() {
        // the single-row functions to be called are in this class
        provider.getEPAdministrator().getConfiguration().addImport(this.getClass());

        // Add an event type that has a byte-type property value
        Map<String, Object> typeDefinition = new HashMap<String, Object>();
        typeDefinition.put("byteValue", byte.class);
        provider.getEPAdministrator().getConfiguration().addEventType("MyByteEvent", typeDefinition);

        // keep the last few events from the variant stream
        EPStatement stmt = provider.getEPAdministrator().createEPL("select RuntimeConfigMain.check2BitSet(byteValue) as check2BitSet from MyByteEvent#lastevent");

        // send an event
        Map<String, Object> eventData = new HashMap<String, Object>();
        eventData.put("byteValue", (byte) 2);
        provider.getEPRuntime().sendEvent(eventData, "MyByteEvent");

        // print results
        System.out.println("\nConfigure Import:");
        System.out.println("Received:" +
                " check2BitSet=" + stmt.iterator().next().get("check2BitSet"));

        // send a second event
        eventData = new HashMap<String, Object>();
        eventData.put("byteValue", (byte) 1);
        provider.getEPRuntime().sendEvent(eventData, "MyByteEvent");

        // print results
        System.out.println("Received:" +
                " check2BitSet=" + stmt.iterator().next().get("check2BitSet"));
    }

    private void configureVariantStream() {

        ConfigurationVariantStream variantStream = new ConfigurationVariantStream();
        variantStream.setTypeVariance(ConfigurationVariantStream.TypeVariance.ANY); // allow any type of event to be inserted into the stream

        // add variant stream
        provider.getEPAdministrator().getConfiguration().addVariantStream("MyVariantStream", variantStream);

        // keep the last few events from the variant stream
        EPStatement stmt = provider.getEPAdministrator().createEPL("select * from MyVariantStream#time(1 min)");

        // insert MyEvent events into the variant stream
        provider.getEPAdministrator().createEPL("insert into MyVariantStream select * from MyEvent");

        // Add a second event type
        Map<String, Object> typeDefinition = new HashMap<String, Object>();
        typeDefinition.put("propertyOther", String.class);
        provider.getEPAdministrator().getConfiguration().addEventType("MyOtherEvent", typeDefinition);

        // insert MyOtherEvent events into the variant stream
        provider.getEPAdministrator().createEPL("insert into MyVariantStream select * from MyOtherEvent");

        // send some events
        Map<String, Object> eventData = new HashMap<String, Object>();
        eventData.put("propertyOne", "value");
        eventData.put("propertyTwo", 10);
        provider.getEPRuntime().sendEvent(eventData, "MyEvent");

        eventData = new HashMap<String, Object>();
        eventData.put("propertyOther", "test");
        provider.getEPRuntime().sendEvent(eventData, "MyOtherEvent");

        // print results
        System.out.println("\nConfigure Variant Stream:");
        Iterator<EventBean> iterator = stmt.iterator();
        EventBean first = iterator.next();
        System.out.println("Received (1):" +
                " propertyOne=" + first.get("propertyOne") +
                " propertyOther=" + first.get("propertyOther"));

        EventBean second = iterator.next();
        System.out.println("Received (2):" +
                " propertyOne=" + second.get("propertyOne") +
                " propertyOther=" + second.get("propertyOther"));
    }

    private void configureRevisionType() {
        // Declare two types: a base type and an update type

        // Define a type for the base events
        Map<String, Object> baseTypeDef = new HashMap<String, Object>();
        baseTypeDef.put("itemId", "string");
        baseTypeDef.put("category", "string");
        baseTypeDef.put("description", "string");
        baseTypeDef.put("price", "double");
        provider.getEPAdministrator().getConfiguration().addEventType("MyBaseEvent", baseTypeDef);

        // Define a type for the update/delta events
        Map<String, Object> updateTypeDef = new HashMap<String, Object>();
        updateTypeDef.put("itemId", "string");
        updateTypeDef.put("price", "double"); // price is updated
        provider.getEPAdministrator().getConfiguration().addEventType("MyUpdateEvent", updateTypeDef);

        // Define revision event type
        ConfigurationRevisionEventType config = new ConfigurationRevisionEventType();
        config.setKeyPropertyNames(new String[]{"itemId"});
        config.addNameBaseEventType("MyBaseEvent");
        config.addNameDeltaEventType("MyUpdateEvent");
        provider.getEPAdministrator().getConfiguration().addRevisionEventType("MyRevisionType", config);

        // Create a statement to keep the last event per item
        EPStatement stmt = provider.getEPAdministrator().createEPL("create window ItemWindow#unique(itemId) select * from MyRevisionType");
        provider.getEPAdministrator().createEPL("insert into ItemWindow select * from MyBaseEvent");
        provider.getEPAdministrator().createEPL("insert into ItemWindow select * from MyUpdateEvent");

        // Send some base events and some update events
        Map<String, Object> baseEvent1 = makeBaseEvent("item1", "stockorder", "GE 100", 20d);
        provider.getEPRuntime().sendEvent(baseEvent1, "MyBaseEvent");

        Map<String, Object> baseEvent2 = makeBaseEvent("item2", "basketorder", "Basket of IBM-100 MSFT-200", 25d);
        provider.getEPRuntime().sendEvent(baseEvent2, "MyBaseEvent");

        Map<String, Object> updateEvent1 = makeUpdateEvent("item1", 21.05d);
        provider.getEPRuntime().sendEvent(updateEvent1, "MyUpdateEvent");

        Map<String, Object> updateEvent2 = makeUpdateEvent("item2", 24.95d);
        provider.getEPRuntime().sendEvent(updateEvent2, "MyUpdateEvent");

        // print results
        System.out.println("\nConfigure Revision Type:");
        Iterator<EventBean> iterator = stmt.iterator();
        EventBean first = iterator.next();
        System.out.println("Received (1):" +
                " itemId=" + first.get("itemId") +
                " category=" + first.get("category") +
                " price=" + first.get("price"));

        EventBean second = iterator.next();
        System.out.println("Received (2):" +
                " itemId=" + second.get("itemId") +
                " category=" + second.get("category") +
                " price=" + second.get("price"));
    }

    private Map<String, Object> makeBaseEvent(String itemId, String category, String description, double price) {
        Map<String, Object> baseEvent = new HashMap<String, Object>();
        baseEvent.put("itemId", itemId);
        baseEvent.put("category", category);
        baseEvent.put("description", description);
        baseEvent.put("price", price);
        return baseEvent;
    }

    private Map<String, Object> makeUpdateEvent(String itemId, double price) {
        Map<String, Object> baseEvent = new HashMap<String, Object>();
        baseEvent.put("itemId", itemId);
        baseEvent.put("price", price);
        return baseEvent;
    }

    public static boolean check2BitSet(byte value) {
        return (value & 2) > 0;
    }

    public static Double computeDoubleMedian(double[] values) {

        if ((values == null) || (values.length == 0)) {
            return null;
        }
        double[] copy = new double[values.length];
        System.arraycopy(values, 0, copy, 0, values.length);
        Arrays.sort(copy);

        if ((copy.length % 2) == 0) {
            int middleIndex = copy.length / 2;
            double left = copy[middleIndex - 1];
            double right = copy[middleIndex];
            return (right + left) / 2.0;
        }

        int middleIndex = copy.length / 2;
        return copy[middleIndex];
    }
}

