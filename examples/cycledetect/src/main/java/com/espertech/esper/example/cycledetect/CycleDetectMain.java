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
package com.espertech.esper.example.cycledetect;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInAggregationMultiFunction;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class CycleDetectMain implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(CycleDetectMain.class);

    private final EPRuntime runtime;

    public static void main(String[] args) {
        new CycleDetectMain().run();
    }

    public CycleDetectMain() {
        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(TransactionEvent.class);

        String[] functionNames = new String[]{CycleDetectorConstant.CYCLEDETECTED_NAME, CycleDetectorConstant.CYCLEOUTPUT_NAME};
        ConfigurationCompilerPlugInAggregationMultiFunction config = new ConfigurationCompilerPlugInAggregationMultiFunction(functionNames, CycleDetectorAggregationForge.class.getName());
        configuration.getCompiler().addPlugInAggregationMultiFunction(config);

        try {
            String eplCycleDetectEachEvent = "@Name('CycleDetector') " +
                "select cycleoutput() as out " +
                "from TransactionEvent#length(1000) " +
                "having cycledetected(fromAcct, toAcct)";
            EPCompiled compiled = EPCompilerProvider.getCompiler().compile(eplCycleDetectEachEvent, new CompilerArguments(configuration));

            runtime = EPRuntimeProvider.getDefaultRuntime(configuration);
            DeploymentOptions deploymentOptions = new DeploymentOptions().setDeploymentId("CycleDetect");
            EPStatement stmt = runtime.getDeploymentService().deploy(compiled, deploymentOptions).getStatements()[0];

            stmt.addListener(new UpdateListener() {
                public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
                    for (EventBean event : newEvents) {
                        @SuppressWarnings("unchecked")
                        Collection<String> accts = (Collection<String>) event.get("out");
                        System.out.println("Cycle detected: " + accts);
                    }
                }
            });
        } catch (Exception ex) {
            throw new RuntimeException("Failed to deploy: " + ex.getMessage(), ex);
        }
    }

    public EPRuntime getRuntime() {
        return runtime;
    }

    public void run() {
        final int range = 1000;
        int count = 0;
        int numEvents = 1000000;

        for (int i = 0; i < numEvents; i++) {

            // Generate random from-account and to-acocunt
            String from;
            String to;
            do {
                from = Integer.toString((int) (Math.random() * range));
                to = Integer.toString((int) (Math.random() * range));
            }
            while (from.equals(to));

            // send event
            // cycles are attempted to be detected for every single invocation
            runtime.getEventService().sendEventBean(new TransactionEvent(from, to, 0), "TransactionEvent");
            count++;

            // every 1000 events send some events that are always a cycle just to test and produce some output
            if (count % 1000 == 0) {
                System.out.println("Processed " + count + " events, sending cycle test events");
                runtime.getEventService().sendEventBean(new TransactionEvent("CycleAssertion__A", "CycleAssertion__B", 0), "TransactionEvent");
                runtime.getEventService().sendEventBean(new TransactionEvent("CycleAssertion__B", "CycleAssertion__A", 0), "TransactionEvent");
            }
        }
    }
}

