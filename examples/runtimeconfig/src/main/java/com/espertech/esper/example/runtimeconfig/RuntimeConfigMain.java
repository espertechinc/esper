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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.common.client.util.NameAccessModifier;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;
import com.espertech.esper.runtime.client.EPStatement;
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

    private EPRuntime runtime;

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
        runtime = EPRuntimeProvider.getDefaultRuntime();

        // Add an event type
        compileDeploy("create schema MyEvent(propertyOne string, propertyTwo int)");

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
    }

    private void configureVariables() {

        compileDeploy("create variable int myintvar = 5");
        EPStatement statement = compileDeploy("select propertyOne, propertyTwo, myintvar from MyEvent#lastevent");

        // send an event
        Map<String, Object> eventData = new HashMap<String, Object>();
        eventData.put("propertyOne", "value");
        eventData.put("propertyTwo", 10);
        runtime.getEventService().sendEventMap(eventData, "MyEvent");

        // the statement above keeps the last event for iterating
        EventBean received = statement.iterator().next();
        System.out.println("\nConfigure Variables:");
        System.out.println("Received:" +
            " propertyOne=" + received.get("propertyOne") +
            " propertyTwo=" + received.get("propertyTwo") +
            " myintvar=" + received.get("myintvar")
        );
    }

    private void configureCustomAggregationFunction() {

        // Add an event type that has a string-type property value
        compileDeploy("create schema MyConcatSampleEvent(str string)");

        // keep the last few events from the variant stream
        EPStatement stmt = compileDeploy("select concat(str) as cstr from MyConcatSampleEvent");

        // send an event
        Map<String, Object> eventData = new HashMap<String, Object>();
        eventData.put("str", "part1");
        runtime.getEventService().sendEventMap(eventData, "MyConcatSampleEvent");

        // print results
        System.out.println("\nConfigure Aggregation Function:");
        System.out.println("Received:" +
            " cstr=" + stmt.iterator().next().get("cstr"));

        // send a second event
        eventData = new HashMap<String, Object>();
        eventData.put("str", "part2");
        runtime.getEventService().sendEventMap(eventData, "MyConcatSampleEvent");

        // print results
        System.out.println("Received:" +
            " cstr=" + stmt.iterator().next().get("cstr"));
    }

    private void configureSingleRowFunction() {

        // Add an event type that has a double-array -type property value
        compileDeploy("create schema MyMedianSampleEvent(doubles double[primitive])");

        // keep the last few events from the variant stream
        EPStatement stmt = compileDeploy("select mymedian(doubles) as med from MyMedianSampleEvent#lastevent");

        // send an event
        Map<String, Object> eventData = new HashMap<String, Object>();
        eventData.put("doubles", new double[]{5, 1, 2, 3, 4});
        runtime.getEventService().sendEventMap(eventData, "MyMedianSampleEvent");

        // print results
        System.out.println("\nConfigure Single-Row Function:");
        System.out.println("Received:" +
            " med=" + stmt.iterator().next().get("med"));

        // send a second event
        eventData = new HashMap<String, Object>();
        eventData.put("doubles", new double[]{5, 1, 2, 3, 4, 4});
        runtime.getEventService().sendEventMap(eventData, "MyMedianSampleEvent");

        // print results
        System.out.println("Received:" +
            " med=" + stmt.iterator().next().get("med"));
    }

    private void configureImport() {
        // Add an event type that has a byte-type property value
        compileDeploy("create schema MyByteEvent(byteValue byte)");

        // keep the last few events from the variant stream
        EPStatement stmt = compileDeploy("select RuntimeConfigMain.check2BitSet(byteValue) as check2BitSet from MyByteEvent#lastevent");

        // send an event
        Map<String, Object> eventData = new HashMap<String, Object>();
        eventData.put("byteValue", (byte) 2);
        runtime.getEventService().sendEventMap(eventData, "MyByteEvent");

        // print results
        System.out.println("\nConfigure Import:");
        System.out.println("Received:" +
            " check2BitSet=" + stmt.iterator().next().get("check2BitSet"));

        // send a second event
        eventData = new HashMap<String, Object>();
        eventData.put("byteValue", (byte) 1);
        runtime.getEventService().sendEventMap(eventData, "MyByteEvent");

        // print results
        System.out.println("Received:" +
            " check2BitSet=" + stmt.iterator().next().get("check2BitSet"));
    }

    private void configureVariantStream() {

        compileDeploy("create variant schema MyVariantStream as *");

        // keep the last few events from the variant stream
        EPStatement stmt = compileDeploy("select * from MyVariantStream#time(1 min)");

        // insert MyEvent events into the variant stream
        compileDeploy("insert into MyVariantStream select * from MyEvent");

        // Add a second event type
        compileDeploy("create schema MyOtherEvent (propertyOther string)");

        // insert MyOtherEvent events into the variant stream
        compileDeploy("insert into MyVariantStream select * from MyOtherEvent");

        // send some events
        Map<String, Object> eventData = new HashMap<String, Object>();
        eventData.put("propertyOne", "value");
        eventData.put("propertyTwo", 10);
        runtime.getEventService().sendEventMap(eventData, "MyEvent");

        eventData = new HashMap<String, Object>();
        eventData.put("propertyOther", "test");
        runtime.getEventService().sendEventMap(eventData, "MyOtherEvent");

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

    private EPStatement compileDeploy(String epl) {
        try {
            Configuration configuration = new Configuration();

            // add sample import for compile-time use
            configuration.getCommon().addImport(RuntimeConfigMain.class);

            // add sample single-row median function provided by this class
            configuration.getCompiler().addPlugInSingleRowFunction("mymedian", this.getClass().getName(), "computeDoubleMedian");

            // add sample append-string aggregation function provided by another class
            configuration.getCompiler().addPlugInAggregationFunctionForge("concat", MyConcatAggregationFunctionForge.class.getName());

            // types and variables shall be available for other statements
            configuration.getCompiler().getByteCode().setAccessModifierEventType(NameAccessModifier.PUBLIC);
            configuration.getCompiler().getByteCode().setAccessModifierVariable(NameAccessModifier.PUBLIC);

            // types shall be available for "sendEvent" use
            configuration.getCompiler().getByteCode().setBusModifierEventType(EventTypeBusModifier.BUS);

            // allow the runtimetypes etc. to be visible to the compiler
            CompilerArguments args = new CompilerArguments(configuration);
            args.getPath().add(runtime.getRuntimePath());

            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(epl, args);
            return runtime.getDeploymentService().deploy(compiled).getStatements()[0];
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

